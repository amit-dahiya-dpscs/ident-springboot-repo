package md.dpscs.cch.iis.service;

import jakarta.annotation.PostConstruct;
import md.dpscs.cch.iis.model.*;
import md.dpscs.cch.iis.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final CountryCodeRepository countryRepo;
    private final CautionCodeRepository cautionRepo;
    private final RaceCodeRepository raceRepo;
    private final SexCodeRepository sexRepo;
    private final EyeCodeRepository eyeRepo;
    private final HairCodeRepository hairRepo;
    private final SkinCodeRepository skinRepo;
    private final Map<String, String> documentTypeMap = new HashMap<>();
    private final List<String> validCodesList = new ArrayList<>();

    @Transactional(readOnly = true)
    @Cacheable("countries")
    public List<CountryCode> getAllCountryCodes() {
        return countryRepo.findAllByOrderByDescriptionAsc();
    }

    @Transactional(readOnly = true)
    @Cacheable("cautions")
    public Map<String, String> getAllCautionCodes() {
        // Convert List<Entity> to Map<Code, Description> for the Frontend
        return cautionRepo.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(
                        CautionCode::getCode,
                        CautionCode::getDescription,
                        (existing, replacement) -> existing // Merge function (keep existing)
                ));
    }

    @Transactional(readOnly = true)
    @Cacheable("races")
    public Map<String, String> getRaceCodes() {
        return raceRepo.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(
                        RaceCode::getCode,
                        RaceCode::getDescription,
                        (existing, replacement) -> existing // Merge function (keep existing)
                ));
    }

    @Transactional(readOnly = true)
    @Cacheable("sexes")
    public Map<String, String> getSexCodes() {
        return sexRepo.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(
                        SexCode::getCode,
                        SexCode::getDescription,
                        (existing, replacement) -> existing // Merge function (keep existing)
                ));
    }

    @Transactional(readOnly = true)
    @Cacheable("eyes")
    public Map<String, String> getEyeColors() {
        return eyeRepo.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(
                        EyeCode::getCode,
                        EyeCode::getDescription,
                        (existing, replacement) -> existing // Merge function (keep existing)
                ));
    }

    @Transactional(readOnly = true)
    @Cacheable("hairs")
    public Map<String, String> getHairColors() {
        return hairRepo.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(
                        HairCode::getCode,
                        HairCode::getDescription,
                        (existing, replacement) -> existing // Merge function (keep existing)
                ));
    }

    @Transactional(readOnly = true)
    @Cacheable("skins")
    public Map<String, String> getSkinTones() {
        return skinRepo.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(
                        SkinCode::getCode,
                        SkinCode::getDescription,
                        (existing, replacement) -> existing // Merge function (keep existing)
                ));
    }

    @PostConstruct
    public void init() {
        initializeReferenceMap();
        validCodesList.addAll(documentTypeMap.keySet());
        Collections.sort(validCodesList);
    }

    /**
     * Loads the strict list of codes derived from IIREFTAB and II0900C logic.
     */
    private void initializeReferenceMap() {
        // --- 1. ARREST Category (SEGARRST) ---
        // Source: IIREFTAB
        addCode("CAR", "ARREST");
        addCode("JUV", "ARREST");

        // --- 2. INDEX Category (SEGINDEX) ---
        // Source: IIREFTAB entries ending in 'SEGINDEX'
        List<String> indexCodes = Arrays.asList(
                "PRR", "BIN", "DOC", "DET", "COF", "SVO", "SVP", "OFF",
                "CSO", "DPP", "DIO", "PAA", "PAB"
        );
        indexCodes.forEach(c -> addCode(c, "INDEX"));

        // Source: IIREFTAB 'DCASEGINDEX' through 'DCZSEGINDEX' (District Courts)
        for (char c = 'A'; c <= 'Z'; c++) {
            addCode("DC" + c, "INDEX");
        }

        // Source: II0900C Logic (Lines 17380) - 209 and 211 are valid for update
        addCode("209", "INDEX");
        addCode("211", "INDEX");

        // --- 3. REFER Category (SEGREFER) ---
        // Source: IIREFTAB entries ending in 'SEGREFER'
        // NOTE: SOR, MAF, PAR, PAV are explicitly REFER in IIREFTAB
        List<String> referCodes = Arrays.asList(
                "GPU", "GPT", "WAR", "PDL", "SPC", "MIS", "APP", "IUR", "MPL", "MPR",
                "PAC", "PAD", "PAE", "PAF", "PAG", "PAH", "PAI", "PAJ",
                "EXP", "PAL", "PAR", "PAV", "SPA", "WAA", "WPL", "WPR", "ROP", "MAF",
                "CIT", "SOR", "FLG", "CNS", "XRF", "ATT", "CCF", "FAD", "EMP", "FFR",
                "GPN", "APS", "APF", "CJS", "APL", "CCD", "MGN", "CJF", "PMD", "SPN",
                "LQF", "REV", "APR", "PDT", "LQM", "VIS", "DJS", "AGE", "ADO", "IDV",
                "RCS", "GPR", "SPR", "GVL", "GVS", "PSC", "HAZ"
        );
        referCodes.forEach(c -> addCode(c, "REFER"));
    }

    private void addCode(String code, String category) {
        documentTypeMap.put(code, category);
    }

    /**
     * Determines the category for a given document type using Business Rules.
     * Logic Priority:
     * 1. Exact Match in IIREFTAB (Map).
     * 2. Dynamic "CC*" rule (Circuit Courts) -> INDEX (II0900C Line 10500).
     * 3. Default/Error.
     */
    public String determineCategory(String docType) {
        if (docType == null) return "REFER";
        String type = docType.toUpperCase().trim();

        // 1. Check Exact Match (Handles exceptions like CCF, CCD which are REFER)
        if (documentTypeMap.containsKey(type)) {
            return documentTypeMap.get(type);
        }

        // 2. Dynamic Logic: Circuit Courts (CC + Year) -> INDEX
        // Source: II0900C 'EDIT-REFER-CC' logic
        if (type.startsWith("CC")) {
            return "INDEX";
        }

        // 3. Fallback for valid codes not explicitly categorized (Safe Default)
        return "REFER";
    }

    /**
     * Validates if a type is allowed for Update/Insert.
     */
    public boolean isValidReferenceType(String docType) {
        if (docType == null) return false;
        String type = docType.toUpperCase().trim();

        // 1. Is it in the explicit list?
        if (documentTypeMap.containsKey(type)) return true;

        // 2. Is it a valid dynamic Circuit Court code? (CC + Char)
        // II0900C allows CC + [A-Z0-9] based on Year Logic
        if (type.startsWith("CC") && type.length() == 3) return true;

        return false;
    }

    public List<String> getReferenceTypeCodes() {
        return validCodesList;
    }
}