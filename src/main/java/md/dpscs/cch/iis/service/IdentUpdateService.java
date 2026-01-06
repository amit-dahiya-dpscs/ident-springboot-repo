package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.dto.*;
import md.dpscs.cch.iis.model.*;
import md.dpscs.cch.iis.repository.*;
import md.dpscs.cch.iis.util.MainframeDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentUpdateService {

    // --- Core Repositories ---
    private final IdentMasterRepository masterRepo;
    private final IdentNameRepository nameRepo;
    private final IdentAddressRepository addressRepo;

    // --- Appended ID Repositories ---
    private final IdentFlagRepository flagRepo;
    private final IdentDobAliasRepository dobRepo;
    private final IdentScarsMarksRepository scarRepo;
    private final IdentSSNRepository ssnRepo;
    private final IdentMiscNumRepository miscNumRepo;
    private final IdentDLRepository dlRepo;

    // --- Reference Repositories ---
    private final IdentDocumentRepository documentRepo;

    // --- Support Services ---
    private final AuditService auditService;
    private ReferenceDataService referenceDataService;
    private final MainframeDataUtils utils;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Updates Core Demographics and Physical Identifiers.
     */
    @Transactional
    public void updateDemographics(Long systemId, UpdateDemographicsRequest request, String username, String ipAddress) {
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found for SystemID: " + systemId));

        // 1. Update Master Record Fields
        master.setRaceCode(request.getRace());
        master.setSexCode(request.getSex());
        master.setHeight(request.getHeight());
        master.setWeight(request.getWeight());
        master.setEyeColorCode(request.getEyeColor());
        master.setHairColorCode(request.getHairColor());
        master.setSkinToneCode(request.getSkinTone());
        master.setPlaceOfBirthCode(request.getPlaceOfBirth());
        master.setCitizenshipCode(request.getCitizenship());
        master.setComments(request.getComments());

        // 2. Logic: UCN/FBI Number Trigger
        if (StringUtils.hasText(request.getFbiNumber())) {
            master.setFbiNumber(request.getFbiNumber());
            master.setRecordType("C"); // Criminal
        } else {
            // Only clear if explicitly sent as empty
            if (request.getFbiNumber() != null) {
                master.setFbiNumber(null);
                master.setRecordType("N"); // Non-Criminal
            }
        }

        master.setLastUpdateDate(LocalDateTime.now());
        masterRepo.save(master);

        // 3. Update Primary Name Record
        IdentName primaryName = getPrimaryName(systemId);
        primaryName.setRaceCode(request.getRace());
        primaryName.setSexCode(request.getSex());

        if (StringUtils.hasText(request.getDob())) {
            primaryName.setDateOfBirth(LocalDate.parse(request.getDob(), DATE_FMT));
        }
        nameRepo.save(primaryName);

        // 4. Update Address
        updateAddress(master, request);

        auditService.logAction(username, ipAddress, "UPDATE_DEMOGRAPHICS", "Updated SID: " + master.getSid());
    }

    /**
     * Updates the True Name (Primary Name).
     */
    @Transactional
    public void updateTrueName(Long systemId, UpdateNameRequest request, String username, String ipAddress) {
        IdentName primaryName = getPrimaryName(systemId);

        // 1. Validation: Conflict Check
        boolean aliasConflict = nameRepo.findByMaster_SystemId(systemId).stream()
                .filter(n -> "A".equals(n.getNameType()))
                .anyMatch(alias -> isSameName(alias, request));

        if (aliasConflict) {
            throw new IllegalArgumentException("This name is already used as an alias.");
        }

        // 2. Update Fields
        primaryName.setLastName(request.getLastName().toUpperCase());
        primaryName.setFirstName(request.getFirstName().toUpperCase());

        String mid = request.getMiddleName() != null ? request.getMiddleName().toUpperCase() : "";
        primaryName.setMiddleName(mid);
        primaryName.setMiddleInitial(!mid.isEmpty() ? mid.substring(0, 1) : "");

        // 3. Recalculate Soundex
        String newSoundex = utils.calculateStandardSoundex(primaryName.getLastName());
        primaryName.setSoundexCode(newSoundex);

        nameRepo.save(primaryName);

        // 4. Update Master Timestamp
        IdentMaster master = primaryName.getMaster();
        master.setLastUpdateDate(LocalDateTime.now());
        masterRepo.save(master);

        auditService.logAction(username, ipAddress, "UPDATE_TRUE_NAME", "Updated Name for SID: " + master.getSid());
    }

    /**
     * Replaces the entire Alias list.
     */
    @Transactional
    public void updateAliases(Long systemId, List<UpdateNameRequest> aliases, String username, String ipAddress) {
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));

        IdentName primaryName = getPrimaryName(systemId);

        // 1. Delete Existing Aliases
        List<IdentName> existingAliases = nameRepo.findByMaster_SystemId(systemId).stream()
                .filter(n -> "A".equals(n.getNameType()))
                .collect(Collectors.toList());
        nameRepo.deleteAll(existingAliases);

        // 2. Insert New Aliases
        if (aliases != null) {
            for (UpdateNameRequest aliasReq : aliases) {
                if (isSameName(primaryName, aliasReq)) {
                    throw new IllegalArgumentException("Alias cannot be the same as the primary name: " + aliasReq.getLastName());
                }

                IdentName newAlias = new IdentName();
                newAlias.setMaster(master);
                newAlias.setNameType("A");
                newAlias.setLastName(aliasReq.getLastName().toUpperCase());
                newAlias.setFirstName(aliasReq.getFirstName().toUpperCase());

                String mid = aliasReq.getMiddleName() != null ? aliasReq.getMiddleName().toUpperCase() : "";
                newAlias.setMiddleName(mid);
                newAlias.setMiddleInitial(!mid.isEmpty() ? mid.substring(0, 1) : "");

                // Aliases inherit Demographics
                newAlias.setRaceCode(primaryName.getRaceCode());
                newAlias.setSexCode(primaryName.getSexCode());
                newAlias.setDateOfBirth(primaryName.getDateOfBirth());
                newAlias.setSoundexCode(utils.calculateStandardSoundex(newAlias.getLastName()));

                nameRepo.save(newAlias);
            }
        }
        auditService.logAction(username, ipAddress, "UPDATE_ALIASES", "Updated " + (aliases != null ? aliases.size() : 0) + " aliases.");
    }

    /**
     * Updates Appended Identifiers.
     */
    @Transactional
    public void updateAppendedIdentifiers(Long systemId, UpdateAppendedIdRequest request, String username, String ipAddress) {
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));

        // --- 1. Caution Codes ---
        if (request.getCautions() != null) {
            List<IdentFlag> existingFlags = flagRepo.findByMaster_SystemId(systemId);
            List<IdentFlag> cautionsToDelete = existingFlags.stream()
                    .filter(f -> "CAUTION".equalsIgnoreCase(f.getFlagType()))
                    .collect(Collectors.toList());
            flagRepo.deleteAll(cautionsToDelete);

            for (String code : request.getCautions()) {
                IdentFlag flag = new IdentFlag();
                flag.setMaster(master);
                flag.setFlagType("CAUTION");
                flag.setFlagCode(code);
                flagRepo.save(flag);
            }
        }

        // --- 2. Alternate DOBs ---
        if (request.getDobs() != null) {
            dobRepo.deleteByMaster_SystemId(systemId);
            for (String dobStr : request.getDobs()) {
                if (StringUtils.hasText(dobStr)) {
                    IdentDobAlias dob = new IdentDobAlias();
                    dob.setMaster(master);
                    dob.setDateOfBirth(LocalDate.parse(dobStr, DATE_FMT));
                    dobRepo.save(dob);
                }
            }
        }

        // --- 3. Scars/Marks/Tattoos ---
        if (request.getScarsMarks() != null) {
            scarRepo.deleteByMaster_SystemId(systemId);
            for (AttributeDTO smt : request.getScarsMarks()) {
                if (StringUtils.hasText(smt.getCode())) {
                    IdentScarsMarks scar = new IdentScarsMarks();
                    scar.setMaster(master);
                    scar.setCode(smt.getCode());
                    scar.setDescription(smt.getDescription());
                    scarRepo.save(scar);
                }
            }
        }

        // --- 4. Social Security Numbers ---
        if (request.getSsns() != null) {
            ssnRepo.deleteByMaster_SystemId(systemId);
            for (String ssnVal : request.getSsns()) {
                if (StringUtils.hasText(ssnVal)) {
                    IdentSSN ssn = new IdentSSN();
                    ssn.setMaster(master);
                    ssn.setSsn(ssnVal.replace("-", ""));
                    ssnRepo.save(ssn);
                }
            }
        }

        // --- 5. Misc Numbers & Drivers Licenses ---
        if (request.getMiscNumbers() != null) {
            miscNumRepo.deleteByMaster_SystemId(systemId);
            dlRepo.deleteByMaster_SystemId(systemId);

            for (SecondaryIDDTO item : request.getMiscNumbers()) {
                if ("DL".equalsIgnoreCase(item.getIdType()) || (item.getIdType() != null && item.getIdType().startsWith("MD"))) {
                    // Driver's License
                    IdentDL dl = new IdentDL();
                    dl.setMaster(master);

                    String raw = item.getIdValue();
                    String state = item.getIdType().length() >= 2 ? item.getIdType().substring(0, 2) : "MD";

                    dl.setStateSource(state);
                    dl.setLicenseNumber(raw);
                    dlRepo.save(dl);
                } else {
                    // Misc Num
                    IdentMiscNum mn = new IdentMiscNum();
                    mn.setMaster(master);
                    mn.setMiscNumType(item.getIdType());
                    mn.setMiscNumber(item.getIdValue());
                    miscNumRepo.save(mn);
                }
            }
        }

        auditService.logAction(username, ipAddress, "UPDATE_APPENDED_ID", "Updated Appended IDs for SID: " + master.getSid());
    }

    /**
     * Updates Reference Data (Replace strategy).
     */
    @Transactional
    public void updateReferenceData(Long systemId, List<DocumentDTO> documents, String username, String ipAddress) {
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found for SystemID: " + systemId));

        // 1. Delete Existing References (Replace Strategy)
        documentRepo.deleteByMaster_SystemId(systemId);

        if (documents != null) {
            for (DocumentDTO docDto : documents) {
                // A. Mandatory Field Checks
                if (docDto.getDocumentDate() == null ||
                        !StringUtils.hasText(docDto.getDocumentType()) ||
                        !StringUtils.hasText(docDto.getDocumentNumber())) {
                    throw new IllegalArgumentException("Reference Date, Type, and Number are required.");
                }

                String type = docDto.getDocumentType().toUpperCase().trim();

                // B. Strict Code Validation (Business Rule)
                // Reject codes not defined in IIREFTAB or dynamic logic
                if (!referenceDataService.isValidReferenceType(type)) {
                    throw new IllegalArgumentException("Invalid Reference Document Type: " + type);
                }

                IdentDocument doc = new IdentDocument();
                doc.setMaster(master);
                doc.setDocumentDate(docDto.getDocumentDate());
                doc.setDocumentType(type);
                doc.setDocumentNumber(docDto.getDocumentNumber().toUpperCase().trim());
                doc.setDescription(docDto.getDescription() != null ? docDto.getDescription().toUpperCase().trim() : "");

                // C. Category Assignment (INDEX / REFER / ARREST)
                String category = referenceDataService.determineCategory(type);
                doc.setDocCategory(category);

                documentRepo.save(doc);
            }
        }

        auditService.logAction(username, ipAddress, "UPDATE_REFERENCES", "Updated references for SID: " + master.getSid());
    }

    // --- Helpers ---

    private IdentName getPrimaryName(Long systemId) {
        return nameRepo.findByMaster_SystemId(systemId).stream()
                .filter(n -> "P".equals(n.getNameType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Primary Name not found"));
    }

    private void updateAddress(IdentMaster master, UpdateDemographicsRequest req) {
        // Corrected logic for Boolean isCurrent
        IdentAddress address = addressRepo.findByMaster_SystemId(master.getSystemId())
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCurrent())) // Correct: Check for Boolean.TRUE
                .findFirst()
                .orElse(new IdentAddress());

        if (address.getAddressId() == null) {
            address.setMaster(master);
            address.setIsCurrent(true); // Correct: Set Boolean
        }

        address.setStreetNumber(req.getStreetNumber());
        address.setStreetDirection(req.getStreetDirection());
        address.setStreetName(req.getStreetName());
        address.setStreetSuffix(req.getStreetSuffix());
        address.setCity(req.getCity());
        address.setStateCode(req.getState());
        address.setZipCode(req.getZip());

        addressRepo.save(address);
    }

    private boolean isSameName(IdentName dbName, UpdateNameRequest req) {
        String dbMid = dbName.getMiddleName() == null ? "" : dbName.getMiddleName().trim();
        String reqMid = req.getMiddleName() == null ? "" : req.getMiddleName().trim();
        return dbName.getLastName().equalsIgnoreCase(req.getLastName()) &&
                dbName.getFirstName().equalsIgnoreCase(req.getFirstName()) &&
                dbMid.equalsIgnoreCase(reqMid);
    }
}