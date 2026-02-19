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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentUpdateService {

    private static final List<String> CRIMINAL_REF_TYPES = List.of(
            "BIN", "DOC", "DIO", "IUR", "PAA", "PAB", "COF", "PAR", "PAV", "PAL",
            "WPR", "WPL", "WAR", "WAA", "DET", "CSO", "SOR", "SVO", "SVP", "OFF",
            "CIE", "PAC", "PAD", "PAE", "PAF", "PAG", "PAH", "PAI", "PAJ",
            "209", "211", "CAR", "CNS", "DPP"
    );

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
    private final IdentFbiDowngradeRepository fbiDowngradeRepo;

    // --- Support Services ---
    private final AuditService auditService;
    private final ReferenceDataService referenceDataService;
    private final MainframeDataUtils utils;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Updates Core Demographics and Physical Identifiers.
     */
    @Transactional
    public void updateDemographics(Long systemId, UpdateDemographicsRequest request, String username, String ipAddress) {
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found for SystemID: " + systemId));

        // --- 1. Mainframe Validation Logic ---
        if (StringUtils.hasText(request.getRace()) && !referenceDataService.isValidRaceCode(request.getRace())) {
            throw new IllegalArgumentException("Invalid Race Code: " + request.getRace());
        }
        if (StringUtils.hasText(request.getSex()) && !referenceDataService.isValidSexCode(request.getSex())) {
            throw new IllegalArgumentException("Invalid Sex Code: " + request.getSex());
        }

        // --- 2. Update Master Record ---
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

        master.setLastUpdateDate(LocalDateTime.now());

        // --- 3. Sync Demographics to ALL Names (Primary AND Aliases) ---
        // FIX: Previously only updated Primary. Now updates all to ensure consistency.
        List<IdentName> allNames = nameRepo.findByMaster_SystemId(systemId);

        // Calculate fields once
        LocalDate newDob = null;
        if (StringUtils.hasText(request.getDob())) {
            newDob = LocalDate.parse(request.getDob(), DATE_FMT);
        }

        // Pattern Type Logic
        String newFp = null;
        // We check if we need to update prints. If both inputs are null, we skip update (preserve existing).
        // If they are empty strings (cleared), we set newFp to null or empty.
        if (request.getPatternRight() != null || request.getPatternLeft() != null) {
            String rawRight = request.getPatternRight() == null ? "" : request.getPatternRight().trim();
            String rawLeft = request.getPatternLeft() == null ? "" : request.getPatternLeft().trim();

            if ((!rawRight.isEmpty() && rawLeft.isEmpty()) || (rawRight.isEmpty() && !rawLeft.isEmpty())) {
                throw new IllegalArgumentException("Invalid Pattern Update: Both Right and Left Pattern Types must be populated or cleared together.");
            }

            String numericRight = utils.convertDisplayToMafisHand(rawRight);
            String numericLeft = utils.convertDisplayToMafisHand(rawLeft);

            if (numericRight.isEmpty() && numericLeft.isEmpty()) {
                newFp = null; // Explicit Removal
            } else {
                newFp = String.format("%-5s%-5s", numericRight, numericLeft);
            }
        }

        // Iterate and Apply
        for (IdentName name : allNames) {
            name.setRaceCode(request.getRace());
            name.setSexCode(request.getSex());

            // Only update DOB if provided in request
            if (StringUtils.hasText(request.getDob())) {
                name.setDateOfBirth(newDob);
            }

            // Only update FP if provided in request
            if (request.getPatternRight() != null || request.getPatternLeft() != null) {
                name.setMafisFingerprint(newFp);
            }
        }

        // Save All Names
        nameRepo.saveAll(allNames);

        // --- 4. Recalculate Record Type ---
        // We can use the primary name for the logic check
        IdentName primaryName = allNames.stream()
                .filter(n -> "P".equals(n.getNameType()))
                .findFirst()
                .orElse(allNames.getFirst());

        recalculateRecordType(master, primaryName);
        masterRepo.save(master);

        // --- 5. Address & Caution Updates ---
        if (request.getCautionFlag() != null) {
            List<String> cautions = StringUtils.hasText(request.getCautionFlag())
                    ? List.of(request.getCautionFlag()) : List.of();
            handleCautions(master, cautions);
        }
        updateAddress(master, request);

        auditService.logAction(username, ipAddress, "UPDATE_DEMOGRAPHICS", "Updated SID: " + master.getSid());
    }

    /**
     * Recalculates the Record Type based on FBI Number, Fingerprints, AND Criminal References.
     * * Logic Hierarchy:
     * 1. CRIMINAL (' '): If Any Criminal Reference exists.
     * 2. PENDING ('T'): If No FBI AND No Fingerprints.
     * 3. NON-CRIMINAL ('N'): If No Criminal Refs, BUT Fingerprints Exist (and was Pending).
     */
    private void recalculateRecordType(IdentMaster master, IdentName primaryName) {
        String fbi = master.getFbiNumber() != null ? master.getFbiNumber().trim() : "";
        String mafis = primaryName.getMafisFingerprint() != null ? primaryName.getMafisFingerprint().trim() : "";

        // Check for existing Criminal Events (PAA, WAR, DOC, etc.)
        boolean hasCrimRef = hasCriminalReferences(master.getSystemId());

        // --- PRIORITY 1: CRIMINAL STATUS (' ') ---
        // If a Criminal Event exists, the record is Criminal.
        if (hasCrimRef) {
            // If currently Pending ('T'), Non-Criminal ('N'), or Juvenile ('J'),
            // upgrade/correct it to Criminal (' ') because a Criminal criteria is met.
            if(fbi.isEmpty() && mafis.isEmpty()) {
                master.setRecordType("T");
            }else{
                master.setRecordType(" ");
            }
            // If already ' ' (Criminal), it remains ' '.
        }

        // --- PRIORITY 2: NO CRIMINAL CRITERIA ---
        // (No FBI Number AND No Criminal References)
        else {
            // Sub-rule A: No Biometrics -> Downgrade to Pending ('T')
            // This handles the "Pattern Removal" scenario for purely civil/empty records.
            if (fbi.isEmpty() && mafis.isEmpty()) {
                master.setRecordType("T");
            }
            // Sub-rule B: Has Biometrics -> Upgrade Pending to Non-Criminal ('N')
            // A record with prints but no criminal data is a valid Civil record.
            else {
                    master.setRecordType("N");
            }
        }
    }

    /**
     * Updates the True Name (Primary Name).
     */
    @Transactional
    public void updateTrueName(Long systemId, UpdateNameRequest request, String username, String ipAddress) {
        IdentName primaryName = getPrimaryName(systemId);
        IdentMaster master = primaryName.getMaster();

        // --- 1. UCN / FBI Number Logic ---
        if (request.getUcn() != null) {
            String newUcn = request.getUcn().trim();
            String currentUcn = master.getFbiNumber();

            // Normalize for comparison
            String safeCurrent = currentUcn == null ? "" : currentUcn;

            if (!newUcn.equals(safeCurrent)) {
                if (StringUtils.hasText(newUcn)) {
                    master.setFbiNumber(newUcn);
                } else {
                    // Before clearing, save the OLD number to the Downgrade Staging Table.
                    if (StringUtils.hasText(currentUcn)) {
                        saveFbiDowngradeStaging(master, currentUcn, username);
                    }
                    master.setFbiNumber(null); // Clearing FBI Number
                }

                // CRITICAL FIX: Recalculate Record Type immediately after changing FBI Number
                recalculateRecordType(master, primaryName);
            }
        }

        // --- 2. Name Conflict Check ---
        boolean aliasConflict = nameRepo.findByMaster_SystemId(systemId).stream()
                .filter(n -> "A".equals(n.getNameType()))
                .anyMatch(alias -> isSameName(alias, request));

        if (aliasConflict) {
            throw new IllegalArgumentException("This name is already used as an alias.");
        }

        // --- 3. Update Name Fields ---
        primaryName.setLastName(request.getLastName().toUpperCase());
        primaryName.setFirstName(request.getFirstName().toUpperCase());

        String mid = request.getMiddleName() != null ? request.getMiddleName().toUpperCase() : "";
        primaryName.setMiddleName(mid);
        primaryName.setMiddleInitial(!mid.isEmpty() ? mid.substring(0, 1) : "");

        // --- 4. Recalculate Soundex ---
        String newSoundex = utils.calculateStandardSoundex(primaryName.getLastName());
        primaryName.setSoundexCode(newSoundex);

        // --- 5. Save & Log ---
        nameRepo.save(primaryName);
        master.setLastUpdateDate(LocalDateTime.now());
        masterRepo.save(master); // Saves the updated RecordType

        auditService.logAction(username, ipAddress, "UPDATE_TRUE_NAME", "Updated Name/UCN for SID: " + master.getSid());
    }

    /**
     * Replaces the entire Alias list.
     */
    @Transactional
    public void updateAliases(Long systemId, List<UpdateNameRequest> aliases, String username, String ipAddress) {
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));

        IdentName primaryName = getPrimaryName(systemId);

        if (aliases != null) {
            for (UpdateNameRequest req : aliases) {
                // --- DELETE LOGIC (Driven by React Flag) ---
                if (Boolean.TRUE.equals(req.getIsMarkedForDeletion())) {
                    if (req.getId() != null) {
                        nameRepo.deleteById(req.getId());
                        auditService.logAction(username, ipAddress, "DELETE_ALIAS", "Deleted Alias ID: " + req.getId());
                    }
                    continue; // Stop processing this record
                }

                // --- VALIDATION: Duplicate Name Check ---
                // (Matches Mainframe II0800C logic which prevents -803 SQL Errors)
                if (isSameName(primaryName, req)) {
                    throw new IllegalArgumentException("Alias cannot be the same as the primary name: " + req.getLastName());
                }

                // Check against existing aliases in DB to prevent duplicates
                // Note: You need to implement this custom query in your Repository
                boolean exists = nameRepo.existsByMaster_SystemIdAndLastNameAndFirstNameAndMiddleNameAndNameType(
                        systemId,
                        req.getLastName().toUpperCase(),
                        req.getFirstName().toUpperCase(),
                        req.getMiddleName() != null ? req.getMiddleName().toUpperCase() : "",
                        "A"
                );

                // Allow update if it's the SAME record (id matches), otherwise block new duplicates
                if (exists && req.getId() == null) {
                    throw new IllegalArgumentException("Duplicate Alias Name: " + req.getLastName() + ", " + req.getFirstName());
                }

                // --- CREATE / UPDATE LOGIC ---
                IdentName aliasToSave;

                if (req.getId() != null) {
                    // Update Existing
                    aliasToSave = nameRepo.findById(req.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Alias ID not found: " + req.getId()));
                } else {
                    // Insert New
                    aliasToSave = new IdentName();
                    aliasToSave.setMaster(master);
                    aliasToSave.setNameType("A");

                    // Mainframe II0800C Requirement: Generate Sequence Number
                    // You need to implement findMaxSequenceBySystemId in Repository
                    Integer maxSeq = nameRepo.findMaxSequenceBySystemId(systemId);
                    aliasToSave.setSequenceNumber(maxSeq != null ? maxSeq + 1 : 1);
                }

                // Map Fields
                aliasToSave.setLastName(req.getLastName().toUpperCase());
                aliasToSave.setFirstName(req.getFirstName().toUpperCase());

                String mid = req.getMiddleName() != null ? req.getMiddleName().toUpperCase() : "";
                aliasToSave.setMiddleName(mid);
                aliasToSave.setMiddleInitial(!mid.isEmpty() ? mid.substring(0, 1) : "");

                // Inherit Demographics from Primary (per FRD/Mainframe logic)
                aliasToSave.setRaceCode(primaryName.getRaceCode());
                aliasToSave.setSexCode(primaryName.getSexCode());
                aliasToSave.setDateOfBirth(primaryName.getDateOfBirth());

                // Calculate Soundex
                aliasToSave.setSoundexCode(utils.calculateStandardSoundex(aliasToSave.getLastName()));

                nameRepo.save(aliasToSave);
            }
        }

        auditService.logAction(username, ipAddress, "UPDATE_ALIASES", "Updated aliases for SID: " + master.getSid());
    }

    /**
     * Updates Appended Identifiers (Caution, DOB, Scars/Marks, SSN, Misc Numbers).
     * <p>
     * <b>Legacy Logic (II0700C):</b>
     * 1. Validates inputs against Reference Tables (PST_SMTCD, etc.).
     * 2. Performs "Delta Detection" (Add vs Delete) rather than bulk replace to preserve history.
     * 3. <b>CRITICAL:</b> If record is on III (Flag1 = 'S' or 'M'), new additions trigger an EHN
     * message to the FBI interface via MainframeDataUtils.
     * </p>
     *
     * @param systemId The internal System ID of the record.
     * @param request  The DTO containing the final state of appended lists.
     */
    @Transactional
    public void updateAppendedIdentifiers(Long systemId, UpdateAppendedIdRequest request, String username, String ipAddress) {
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found for SystemID: " + systemId));

        // Legacy II0700C: Determine if record is on Interstate Identification Index (III)
        // Flag1 'S' (Single State) or 'M' (Multi-State) requires FBI Notification.
        //"S".equalsIgnoreCase(master.getFlag1()) || "M".equalsIgnoreCase(master.getFlag1());
        boolean isOnIII = false;

        // --- 1. Process Caution Codes ---
        // FRD Req: Dropdown selection, "A-M". No III notification required for Cautions.
        handleCautions(master, request.getCautions());

        // --- 2. Process Alternate DOBs ---
        // FRD Req: Valid Date, Year >= 1900, Not Future.
        // Legacy Req: Trigger EHN message if isOnIII.
        handleDobs(master, request.getDobs(), isOnIII, username, ipAddress);

        // --- 3. Process Scars/Marks/Tattoos (SMT) ---
        // FRD Req: Max 10 chars, Alphanumeric+Space.
        // Legacy Req: Validate against PST_SMTCD, Trigger EHN if isOnIII.
        handleScarsMarks(master, request.getScarsMarks(), isOnIII, username, ipAddress);

        // --- 4. Process Social Security Numbers (SSN) ---
        // FRD Req: 9 digits, not blank.
        // Legacy Req: Trigger EHN if isOnIII.
        handleSsns(master, request.getSsns(), isOnIII, username, ipAddress);

        // --- 5. Process Misc Numbers & Drivers Licenses ---
        // FRD Req: Valid Prefix (AF, AR, etc.), 12-digit Number.
        // Legacy Req: Trigger EHN if isOnIII.
        handleMiscNumbers(master, request.getMiscNumbers(), isOnIII, username, ipAddress);

        // --- Final Transaction Audit ---
        auditService.logAction(username, ipAddress, "UPDATE_APPENDED_ID",
                "Updated Appended IDs for SID: " + master.getSid());
    }

    // ==================================================================================
    // PRIVATE HELPER METHODS (Delta Detection & Business Logic)
    // ==================================================================================

    private void handleCautions(IdentMaster master, List<String> incomingCodes) {
        if (incomingCodes == null) return;

        List<IdentFlag> existingFlags = flagRepo.findByMaster_SystemId(master.getSystemId()).stream()
                .filter(f -> "CAUTION".equalsIgnoreCase(f.getFlagType()))
                .toList();

        // 1. Identify Deletes (In DB but not in Request)
        List<IdentFlag> toDelete = existingFlags.stream()
                .filter(db -> incomingCodes.stream().noneMatch(req -> req.equalsIgnoreCase(db.getFlagCode())))
                .collect(Collectors.toList());
        flagRepo.deleteAll(toDelete);

        // 2. Identify Adds (In Request but not in DB)
        List<String> toAdd = incomingCodes.stream()
                .filter(req -> existingFlags.stream().noneMatch(db -> db.getFlagCode().equalsIgnoreCase(req)))
                .toList();

        for (String code : toAdd) {
            // FRD Validation: "A valid caution must be selected."
            if (!StringUtils.hasText(code)) {
                throw new IllegalArgumentException("Caution code cannot be blank.");
            }
            // Validation against Reference Data
            if (!referenceDataService.isValidCautionCode(code)) {
                throw new IllegalArgumentException("Invalid Caution Code: " + code);
            }

            IdentFlag flag = new IdentFlag();
            flag.setMaster(master);
            flag.setFlagType("CAUTION");
            flag.setFlagCode(code);
            flagRepo.save(flag);
        }
    }

    private void handleDobs(IdentMaster master, List<String> incomingDobs, boolean isOnIII, String user, String ip) {
        if (incomingDobs == null) return;

        List<IdentDobAlias> existingDobs = dobRepo.findByMaster_SystemId(master.getSystemId());

        // 1. Identify Deletes
        List<IdentDobAlias> toDelete = existingDobs.stream()
                .filter(db -> incomingDobs.stream().noneMatch(req ->
                        db.getDateOfBirth() != null && req.equals(db.getDateOfBirth().format(DATE_FMT))))
                .collect(Collectors.toList());
        dobRepo.deleteAll(toDelete);

        // 2. Identify Adds
        List<String> toAdd = incomingDobs.stream()
                .filter(req -> existingDobs.stream().noneMatch(db ->
                        db.getDateOfBirth() != null && req.equals(db.getDateOfBirth().format(DATE_FMT))))
                .toList();

        for (String dobStr : toAdd) {
            if (!StringUtils.hasText(dobStr)) continue;

            LocalDate dob;
            try {
                dob = LocalDate.parse(dobStr, DATE_FMT);
            } catch (Exception e) {
                throw new IllegalArgumentException("Date must be in MM/dd/yyyy format: " + dobStr);
            }

            // FRD Validation: "The year in the date should not be before 1900"
            if (dob.getYear() < 1900) {
                throw new IllegalArgumentException("DOB year cannot be earlier than 1900: " + dobStr);
            }
            // FRD Validation: "Date cannot be today or a future date"
            if (!dob.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("DOB cannot be today or in the future: " + dobStr);
            }

            IdentDobAlias alias = new IdentDobAlias();
            alias.setMaster(master);
            alias.setDateOfBirth(dob);
            dobRepo.save(alias);

            // Legacy III Sync
//            if (isOnIII) {
//                String iiiMsg = "DOB/" + dobStr.replace("/", ""); // Format MMDDYY
//                utils.sendEhnMessage(master.getSid(), master.getFbiNumber(), iiiMsg);
//                auditService.logAction(user, ip, "III_NOTIFICATION_SENT", "Sent EHN: " + iiiMsg);
//            }
        }
    }

    private void handleScarsMarks(IdentMaster master, List<AttributeDTO> incomingSmts, boolean isOnIII, String user, String ip) {
        if (incomingSmts == null) return;

        List<IdentScarsMarks> existingSmts = scarRepo.findByMaster_SystemId(master.getSystemId());

        // 1. Identify Deletes (Compare by Code)
        List<IdentScarsMarks> toDelete = existingSmts.stream()
                .filter(db -> incomingSmts.stream().noneMatch(req -> req.getCode().equalsIgnoreCase(db.getCode())))
                .collect(Collectors.toList());
        scarRepo.deleteAll(toDelete);

        // 2. Identify Adds
        List<AttributeDTO> toAdd = incomingSmts.stream()
                .filter(req -> existingSmts.stream().noneMatch(db -> db.getCode().equalsIgnoreCase(req.getCode())))
                .toList();

        for (AttributeDTO smt : toAdd) {
            String code = smt.getCode();

            // FRD Validation: "Scar/Mark cannot be blank"
            if (!StringUtils.hasText(code)) {
                throw new IllegalArgumentException("Scar/Mark code cannot be blank.");
            }
            // FRD Validation: "Code cannot exceed 10 characters"
            if (code.length() > 10) {
                throw new IllegalArgumentException("Scar/Mark code cannot exceed 10 characters: " + code);
            }
            // FRD Validation: "Code can only contain letters, numbers, and spaces"
            if (!code.matches("^[a-zA-Z0-9 ]*$")) {
                throw new IllegalArgumentException("Scar/Mark contains invalid characters: " + code);
            }
            // Legacy Validation: Check against PST_SMTCD reference table
            if (!referenceDataService.isValidSmtCode(code)) {
                throw new IllegalArgumentException("Invalid Scar/Mark code (not in reference table): " + code);
            }

            IdentScarsMarks entity = new IdentScarsMarks();
            entity.setMaster(master);
            entity.setCode(code.toUpperCase());
            entity.setDescription(smt.getDescription() != null ? smt.getDescription().toUpperCase() : "");
            entity.setCreateTimestamp(LocalDateTime.now());
            scarRepo.save(entity);

            // Legacy III Sync
//            if (isOnIII) {
//                String iiiMsg = "SMT/" + code.toUpperCase();
//                utils.sendEhnMessage(master.getSid(), master.getFbiNumber(), iiiMsg);
//                auditService.logAction(user, ip, "III_NOTIFICATION_SENT", "Sent EHN: " + iiiMsg);
//            }
        }
    }

    private void handleSsns(IdentMaster master, List<String> incomingSsns, boolean isOnIII, String user, String ip) {
        if (incomingSsns == null) return;

        List<IdentSSN> existingSsns = ssnRepo.findByMaster_SystemId(master.getSystemId());

        // 1. Identify Deletes (Normalize to raw digits for comparison)
        List<IdentSSN> toDelete = existingSsns.stream()
                .filter(db -> incomingSsns.stream().noneMatch(req ->
                        req.replace("-", "").equals(db.getSsn())))
                .collect(Collectors.toList());
        ssnRepo.deleteAll(toDelete);

        // 2. Identify Adds
        List<String> toAdd = incomingSsns.stream()
                .filter(req -> existingSsns.stream().noneMatch(db ->
                        db.getSsn().equals(req.replace("-", ""))))
                .toList();

        for (String rawSsn : toAdd) {
            String cleanSsn = rawSsn.replace("-", "").trim();

            // FRD Validation: "SSN cannot be blank"
            if (!StringUtils.hasText(cleanSsn)) {
                throw new IllegalArgumentException("SSN cannot be blank.");
            }
            // FRD Validation: "Incomplete SSN" (Must be 9 digits)
            if (cleanSsn.length() != 9 || !cleanSsn.matches("\\d+")) {
                throw new IllegalArgumentException("Incomplete or invalid SSN: " + rawSsn);
            }

            IdentSSN entity = new IdentSSN();
            entity.setMaster(master);
            entity.setSsn(cleanSsn);
            ssnRepo.save(entity);

            // Legacy III Sync
//            if (isOnIII) {
//                String iiiMsg = "SOC/" + cleanSsn;
//                utils.sendEhnMessage(master.getSid(), master.getFbiNumber(), iiiMsg);
//                auditService.logAction(user, ip, "III_NOTIFICATION_SENT", "Sent EHN: " + iiiMsg);
//            }
        }
    }

    private void handleMiscNumbers(IdentMaster master, List<SecondaryIDDTO> incomingMisc, boolean isOnIII, String user, String ip) {
        if (incomingMisc == null) return;

        // Fetch both Misc Tables (General Misc + Driver License are often handled together in UI)
        List<IdentMiscNum> existingMisc = miscNumRepo.findByMaster_SystemId(master.getSystemId());
        List<IdentDL> existingDLs = dlRepo.findByMaster_SystemId(master.getSystemId());

        // Note: For simplicity in this method, we are focusing on the Generic Misc Numbers
        // as typically DLs are handled in a separate specific logic block in Mainframe,
        // but FRD v2.0 groups them under 'MISC-NUMBER'.
        // Assuming 'MD-' etc are stored in T_IDENT_MISC_NUM for this implementation.

        // 1. Identify Deletes
        List<IdentMiscNum> toDelete = existingMisc.stream()
                .filter(db -> incomingMisc.stream().noneMatch(req ->
                        req.getIdType().equalsIgnoreCase(db.getMiscNumType()) &&
                                req.getIdValue().equalsIgnoreCase(db.getMiscNumber())))
                .collect(Collectors.toList());
        miscNumRepo.deleteAll(toDelete);

        // 2. Identify Adds
        List<SecondaryIDDTO> toAdd = incomingMisc.stream()
                .filter(req -> existingMisc.stream().noneMatch(db ->
                        db.getMiscNumType().equalsIgnoreCase(req.getIdType()) &&
                                db.getMiscNumber().equalsIgnoreCase(req.getIdValue())))
                .toList();

        for (SecondaryIDDTO dto : toAdd) {
            String prefix = dto.getIdType(); // e.g., "AF", "MD"
            String number = dto.getIdValue();

            // FRD Validation: "Both prefix and number are required"
            if (!StringUtils.hasText(prefix) || !StringUtils.hasText(number)) {
                throw new IllegalArgumentException("Misc Number requires both Prefix and Number.");
            }
            // FRD Validation: "Invalid Prefix"
                if (!referenceDataService.isValidMiscPrefix(prefix)) {
                throw new IllegalArgumentException("Invalid Misc Number Prefix: " + prefix);
            }
            // FRD Validation (Length): Usually 12 digits per FRD screenshot
            if (number.length() > 12) {
                throw new IllegalArgumentException("Misc Number cannot exceed 12 characters.");
            }

            IdentMiscNum entity = new IdentMiscNum();
            entity.setMaster(master);
            entity.setMiscNumType(prefix.toUpperCase());
            entity.setMiscNumber(number.toUpperCase());
            miscNumRepo.save(entity);

            // Legacy III Sync
//            if (isOnIII) {
//                // Format: MNU/PP-NNNNNNNN (Prefix-Number)
//                String iiiMsg = "MNU/" + prefix.toUpperCase() + "-" + number.toUpperCase();
//                utils.sendEhnMessage(master.getSid(), master.getFbiNumber(), iiiMsg);
//                auditService.logAction(user, ip, "III_NOTIFICATION_SENT", "Sent EHN: " + iiiMsg);
//            }
        }
    }

    /** Updates Reference Data.
     * * BUSINESS RULES (FRD v2.0 / II0900C):
     * 1. INSERT: Allowed for new records (id == null).
     * 2. UPDATE: Allowed for existing records (id != null), but ONLY 'Description' can be changed.
     * The Key Fields (Date, Type, Number) are immutable.
     * 3. DELETE: NOT ALLOWED in this transaction. Deletion is an Expungement workflow function.
     * (Missing records in the list are ignored, not deleted).
     */
    @Transactional
    public void updateReferenceData(Long systemId, List<DocumentDTO> documents, String username, String ipAddress) {
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found for SystemID: " + systemId));

        if (documents == null) return;

        IdentName primaryName = getPrimaryName(systemId);

        for (DocumentDTO docDto : documents) {
            if (docDto.getId() != null) {
                // UPDATE EXISTING (Description Only)
                IdentDocument existingDoc = documentRepo.findById(docDto.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Reference Doc ID not found: " + docDto.getId()));

                if (!existingDoc.getMaster().getSystemId().equals(systemId)) {
                    throw new IllegalArgumentException("Security Mismatch: Document does not belong to this record.");
                }

                String newDesc = docDto.getDescription() != null ? docDto.getDescription().toUpperCase().trim() : "";
                if (!newDesc.equals(existingDoc.getDescription())) {
                    existingDoc.setDescription(newDesc);
                    documentRepo.save(existingDoc);
                }

            } else {
                // INSERT NEW RECORD
                if (docDto.getDocumentDate() == null ||
                        !StringUtils.hasText(docDto.getDocumentType()) ||
                        !StringUtils.hasText(docDto.getDocumentNumber())) {
                    throw new IllegalArgumentException("Reference Date, Type, and Number are required.");
                }

                String type = docDto.getDocumentType().toUpperCase().trim();
                String number = docDto.getDocumentNumber().toUpperCase().trim();
                LocalDate date = docDto.getDocumentDate();

                if (!referenceDataService.isValidReferenceType(type)) {
                    throw new IllegalArgumentException("Invalid Reference Document Type: " + type);
                }

                boolean exists = documentRepo.existsByMaster_SystemIdAndDocumentTypeAndDocumentNumberAndDocumentDate(
                        systemId, type, number, date);

                if (exists) {
                    throw new IllegalArgumentException(
                            String.format("Duplicate Reference found: Type '%s', Number '%s', Date '%s' already exists.",
                                    type, number, date));
                }

                IdentDocument doc = new IdentDocument();
                doc.setMaster(master);
                doc.setDocumentDate(docDto.getDocumentDate());
                doc.setDocumentType(type);
                doc.setDocumentNumber(docDto.getDocumentNumber().toUpperCase().trim());
                doc.setDescription(docDto.getDescription() != null ? docDto.getDescription().toUpperCase().trim() : "");

                String category = referenceDataService.determineCategory(type);
                doc.setDocCategory(category);

                documentRepo.save(doc);

                recalculateRecordType(master, primaryName);
                masterRepo.save(master);
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

    private boolean hasCriminalReferences(Long systemId) {
        // Query the consolidated IdentDocumentRepository for this systemId
        List<IdentDocument> docs = documentRepo.findByMaster_SystemId(systemId);

        // Check if any document type matches the criminal list
        return docs.stream().anyMatch(doc ->
                doc.getDocumentType() != null &&
                        CRIMINAL_REF_TYPES.contains(doc.getDocumentType().toUpperCase().trim())
        );
    }

    private void saveFbiDowngradeStaging(IdentMaster master, String deletedFbiNumber, String username) {
        // 1. Check for existing record (II1100C Lines 5101-5106)
        Optional<IdentFbiDowngrade> existingRecord = fbiDowngradeRepo.findBySystemIdAndSidAndFbiNumberAndFbiRecordIndicator(
                master.getSystemId(),
                master.getSid(),
                deletedFbiNumber,
                "N" // 'N' indicates it is a Staged/Pending downgrade
        );

        IdentFbiDowngrade log;

        if (existingRecord.isPresent()) {
            // --- UPDATE EXISTING (II1100C Lines 5124-5132) ---
            log = existingRecord.get();
            // In COBOL UPDATE-FBDGR, it updates the timestamp and user.
            // It also updates demographics if they changed, though typically they persist.
        } else {
            // --- INSERT NEW (II1100C Lines 5114-5118) ---
            log = new IdentFbiDowngrade();
            log.setSid(master.getSid());
            log.setFbiNumber(deletedFbiNumber);
            log.setSystemId(master.getSystemId());
            log.setFbiRecordIndicator("N"); // 'N' = Staged
        }

        // Populate Demographics from Primary Name (Snapshot at time of removal)
        // See II1100C Lines 5092-5094
        IdentName pName = getPrimaryName(master.getSystemId());
        log.setLastName(pName.getLastName());
        log.setFirstName(pName.getFirstName());
        log.setMiddleName(pName.getMiddleName());

        // Populate SSN (II1100C Lines 5095)
        ssnRepo.findByMaster_SystemId(master.getSystemId()).stream()
                .findFirst()
                .ifPresent(s -> log.setSsn(s.getSsn()));

        // --- COMMON UPDATES (For both Insert and Update) ---
        log.setUserId(username);
        // Save to Database
        fbiDowngradeRepo.save(log);
    }
}