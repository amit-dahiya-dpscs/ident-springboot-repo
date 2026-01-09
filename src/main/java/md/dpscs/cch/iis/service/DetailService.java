package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.dto.*;
import md.dpscs.cch.iis.model.*;
import md.dpscs.cch.iis.repository.*;
import md.dpscs.cch.iis.util.MainframeDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetailService {

    // --- Repositories ---
    private final IdentMasterRepository masterRepo;
    private final IdentNameRepository nameRepo;
    private final IdentAddressRepository addrRepo;
    private final IdentSSNRepository ssnRepo;
    private final IdentDLRepository dlRepo;
    private final IdentMiscNumRepository miscRepo;
    private final IdentDobAliasRepository dobRepo;
    private final IdentFlagRepository flagRepo;
    private final IdentScarsMarksRepository scarsRepo;
    private final IdentHenryFPRepository henryRepo;
    private final IdentNcicFPRepository ncicRepo;
    private final IdentDocumentRepository docRepo;

    // --- Services & Utilities ---
    private final ReferenceDataService refService;
    private final MainframeDataUtils utils;

    @Transactional(readOnly = true)
    public PersonDetailDTO getPersonDetails(Long systemId) {

        // 1. Fetch Core Master Record
        IdentMaster master = masterRepo.findById(systemId)
                .orElseThrow(() -> new RuntimeException("Record not found for SystemID: " + systemId));

        // 2. Execute Parallel Fetches
        var namesFut = CompletableFuture.supplyAsync(() -> nameRepo.findByMaster_SystemId(systemId));
        var addrFut = CompletableFuture.supplyAsync(() -> addrRepo.findByMaster_SystemId(systemId));
        var ssnFut = CompletableFuture.supplyAsync(() -> ssnRepo.findByMaster_SystemId(systemId));
        var dlFut = CompletableFuture.supplyAsync(() -> dlRepo.findByMaster_SystemId(systemId));
        var miscFut = CompletableFuture.supplyAsync(() -> miscRepo.findByMaster_SystemId(systemId));
        var dobFut = CompletableFuture.supplyAsync(() -> dobRepo.findByMaster_SystemId(systemId));
        var flagFut = CompletableFuture.supplyAsync(() -> flagRepo.findByMaster_SystemId(systemId));
        var scarFut = CompletableFuture.supplyAsync(() -> scarsRepo.findByMaster_SystemId(systemId));
        var henryFut = CompletableFuture.supplyAsync(() -> henryRepo.findByMaster_SystemId(systemId));
        var ncicFut = CompletableFuture.supplyAsync(() -> ncicRepo.findByMaster_SystemId(systemId));
        var docsFut = CompletableFuture.supplyAsync(() -> docRepo.findByMaster_SystemIdOrderByDocumentDateDesc(systemId));

        // 3. Join threads
        CompletableFuture.allOf(namesFut, addrFut, ssnFut, dlFut, miscFut, dobFut, flagFut, scarFut, henryFut, ncicFut, docsFut).join();

        // 4. Load Reference Data
        Map<String, String> cautionMap = refService.getAllCautionCodes();

        // 5. Assemble DTO
        PersonDetailDTO dto = new PersonDetailDTO();

        // --- Header Information ---
        dto.setSystemId(master.getSystemId());
        dto.setSid(master.getSid());
        dto.setFbiNumber(master.getFbiNumber());

        // FIX: Map Record Type Code to Description (Source 1785)
        dto.setRecordType(mapRecordType(master.getRecordType()));

        dto.setLastUpdated(master.getLastUpdateDate());
        dto.setComments(master.getComments());

        // --- Personal Identifiers ---
        dto.setRace(master.getRaceCode());
        dto.setSex(master.getSexCode());
        dto.setHeight(master.getHeight());
        dto.setWeight(master.getWeight());
        dto.setEyeColor(master.getEyeColorCode());
        dto.setHairColor(master.getHairColorCode());
        dto.setSkinTone(master.getSkinToneCode());
        dto.setPlaceOfBirth(master.getPlaceOfBirthCode());
        dto.setCitizenship(master.getCitizenshipCode());

        // --- Mapped Lists ---
        dto.setNamesAndAliases(namesFut.join().stream().map(this::mapName).collect(Collectors.toList()));
        dto.setAddressHistory(addrFut.join().stream().map(this::mapAddress).collect(Collectors.toList()));
        dto.setSsnHistory(ssnFut.join().stream().map(this::mapSsn).collect(Collectors.toList()));
        dto.setDriverLicenses(dlFut.join().stream().map(this::mapDl).collect(Collectors.toList()));
        dto.setSecondaryIdentifiers(miscFut.join().stream().map(this::mapMisc).collect(Collectors.toList()));
        dto.setAlternateDOBs(dobFut.join().stream().map(this::mapDob).collect(Collectors.toList()));
        dto.setScarsAndMarks(scarFut.join().stream().map(this::mapScar).collect(Collectors.toList()));

        // --- Map Flags & Cautions ---
        dto.setFlags(flagFut.join().stream().map(f -> mapFlag(f, cautionMap)).collect(Collectors.toList()));

        // 1. Map Misc Numbers
        List<SecondaryIDDTO> miscList = miscFut.join().stream()
                .map(this::mapMisc)
                .collect(Collectors.toList());

        // 2. Map Driver's Licenses as "Misc Numbers" (Type = "DL")
        List<SecondaryIDDTO> dlAsMisc = dlFut.join().stream()
                .map(this::mapDlToMisc)
                .collect(Collectors.toList());

        // 3. Combine them
        miscList.addAll(dlAsMisc);
        dto.setSecondaryIdentifiers(miscList);

        // (Optional) Keep dedicated DL list if needed for other logic, otherwise this is redundant but harmless
        dto.setDriverLicenses(dlFut.join().stream().map(this::mapDl).collect(Collectors.toList()));

        // --- Fingerprint Classifications ---
        List<FingerprintDTO> fpList = new ArrayList<>();
        henryFut.join().forEach(h -> fpList.add(mapHenry(h)));
        ncicFut.join().forEach(n -> fpList.add(mapNcic(n)));
        dto.setFingerprints(fpList);

        // --- Fingerprint Pattern Type (MAFIS) ---
        IdentName primaryName = dto.getNamesAndAliases().isEmpty() ? null :
                namesFut.join().stream().filter(n -> "P".equals(n.getNameType())).findFirst().orElse(namesFut.join().get(0));

        if (primaryName != null && primaryName.getMafisFingerprint() != null) {
            String rawFp = primaryName.getMafisFingerprint();
            String convertedFp = utils.convertMafisHandToDisplay(rawFp);
            String paddedFp = String.format("%-10s", convertedFp);
            dto.setPatternRight(paddedFp.substring(0, 5).trim());
            dto.setPatternLeft(paddedFp.substring(5, Math.min(10, paddedFp.length())).trim());
        } else {
            dto.setPatternRight("");
            dto.setPatternLeft("");
        }

        // --- Documents ---
        List<IdentDocument> allDocs = docsFut.join();
        dto.setArrestDocuments(allDocs.stream().filter(d -> "ARREST".equalsIgnoreCase(d.getDocCategory())).map(this::mapDoc).collect(Collectors.toList()));
        dto.setIndexDocuments(allDocs.stream().filter(d -> "INDEX".equalsIgnoreCase(d.getDocCategory())).map(this::mapDoc).collect(Collectors.toList()));
        dto.setGeneralReferences(allDocs.stream().filter(d -> "REFER".equalsIgnoreCase(d.getDocCategory())).map(this::mapDoc).collect(Collectors.toList()));

        // --- Derived Caution Flag ---
        String primaryCaution = dto.getFlags().stream()
                .filter(f -> "CAUTION".equals(f.getType()))
                .findFirst()
                .map(FlagDTO::getDescription)
                .orElse("NONE");
        dto.setCautionFlag(primaryCaution);

        return dto;
    }

    private SecondaryIDDTO mapDlToMisc(IdentDL entity) {
        SecondaryIDDTO dto = new SecondaryIDDTO();
        dto.setIdType("DL");
        String state = entity.getStateSource() != null ? entity.getStateSource().trim() : "";
        String num = entity.getLicenseNumber() != null ? entity.getLicenseNumber().trim() : "";
        dto.setIdValue((state + num).trim());
        return dto;
    }

    // --- NEW HELPER: Record Type Mapping ---
    private String mapRecordType(String code) {
        if (code == null) return "CRIMINAL";
        return switch (code.toUpperCase()) {
            case "J" -> "JUVENILE";
            case "N" -> "NON-CRIMINAL";
            case "P" -> "PENDING";
            case "F" -> "FLYER";
            default -> "CRIMINAL";
        };
    }

    // --- Mappers (Existing) ---
    private NameDTO mapName(IdentName entity) {
        NameDTO dto = new NameDTO();
        dto.setId(entity.getNameId());
        dto.setNameType(entity.getNameType());
        dto.setLastName(entity.getLastName());
        dto.setFirstName(entity.getFirstName());
        dto.setMiddleName(entity.getMiddleName());
        dto.setMiddleInitial(entity.getMiddleInitial());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setRace(entity.getRaceCode());
        dto.setSex(entity.getSexCode());
        return dto;
    }

    private AddressDTO mapAddress(IdentAddress entity) {
        AddressDTO dto = new AddressDTO();
        dto.setStreetNumber(entity.getStreetNumber());
        dto.setStreetDirection(entity.getStreetDirection());
        dto.setStreetName(entity.getStreetName());
        dto.setStreetSuffix(entity.getStreetSuffix());
        dto.setCity(entity.getCity());
        dto.setState(entity.getStateCode());
        dto.setZip(entity.getZipCode());
        dto.setIsCurrent(entity.getIsCurrent());

        String full = (entity.getStreetNumber() != null ? entity.getStreetNumber() + " " : "") +
                (entity.getStreetDirection() != null ? entity.getStreetDirection() + " " : "") +
                (entity.getStreetName() != null ? entity.getStreetName() + " " : "") +
                (entity.getStreetSuffix() != null ? entity.getStreetSuffix() : "");
        dto.setFullAddress(full.trim());
        return dto;
    }

    private SsnDTO mapSsn(IdentSSN entity) {
        SsnDTO dto = new SsnDTO();
        dto.setSsn(entity.getSsn());
        dto.setDateRecorded(entity.getDateRecorded() != null ? entity.getDateRecorded().toLocalDate() : null);
        return dto;
    }

    private DriverLicenseDTO mapDl(IdentDL entity) {
        DriverLicenseDTO dto = new DriverLicenseDTO();
        dto.setLicenseNumber(entity.getLicenseNumber());
        dto.setStateCode(entity.getStateSource());
        return dto;
    }

    private SecondaryIDDTO mapMisc(IdentMiscNum entity) {
        SecondaryIDDTO dto = new SecondaryIDDTO();

        dto.setIdType(entity.getMiscNumType());
        String type = entity.getMiscNumType() != null ? entity.getMiscNumType().trim() : "";
        String num = entity.getMiscNumber() != null ? entity.getMiscNumber().trim() : "";

        dto.setIdValue((type + num).trim());

        return dto;
    }

    private AltDOBDTO mapDob(IdentDobAlias entity) {
        AltDOBDTO dto = new AltDOBDTO();
        dto.setDob(entity.getDateOfBirth());
        return dto;
    }

    private FlagDTO mapFlag(IdentFlag entity, Map<String, String> cautionMap) {
        FlagDTO dto = new FlagDTO();
        dto.setCode(entity.getFlagCode());
        dto.setType(entity.getFlagType());
        String desc = cautionMap.getOrDefault(entity.getFlagCode(), entity.getFlagCode());
        dto.setDescription(desc);
        return dto;
    }

    private AttributeDTO mapScar(IdentScarsMarks entity) {
        AttributeDTO dto = new AttributeDTO();
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private FingerprintDTO mapHenry(IdentHenryFP entity) {
        FingerprintDTO dto = new FingerprintDTO();
        dto.setType("HENRY");
        dto.setPrimary(entity.getPrimaryHenry());
        dto.setClassification(entity.getFpcHenry());
        return dto;
    }

    private FingerprintDTO mapNcic(IdentNcicFP entity) {
        FingerprintDTO dto = new FingerprintDTO();
        dto.setType("NCIC");
        dto.setClassification(entity.getFpcPattern());
        return dto;
    }

    private DocumentDTO mapDoc(IdentDocument entity) {
        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentType(entity.getDocumentType());
        dto.setDocumentNumber(entity.getDocumentNumber());
        dto.setDocumentDate(entity.getDocumentDate());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getDocCategory());
        return dto;
    }
}