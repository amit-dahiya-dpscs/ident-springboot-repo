package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.dto.SearchCriteriaDTO;
import md.dpscs.cch.iis.dto.SearchResultDTO;
import md.dpscs.cch.iis.model.IdentMaster;
import md.dpscs.cch.iis.model.IdentName;
import md.dpscs.cch.iis.repository.IdentNameRepository;
import md.dpscs.cch.iis.util.MainframeDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final IdentNameRepository nameRepo;
    private final MainframeDataUtils utils;

    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    @Transactional(readOnly = true)
    public Page<SearchResultDTO> executeSearch(SearchCriteriaDTO criteria, Pageable pageable) {

        // 1. Sanitize Inputs
        String race = StringUtils.hasText(criteria.getRace()) ? criteria.getRace().toUpperCase() : null;
        String sex = StringUtils.hasText(criteria.getSex()) ? criteria.getSex().toUpperCase() : null;

        // 2. Parse Date Range (Supports MM/dd/yyyy OR yyyy)
        DateRange dateRange = parseDobRange(criteria.getDob());
        LocalDate startDob = dateRange != null ? dateRange.start : null;
        LocalDate endDob = dateRange != null ? dateRange.end : null;

        // --- PRIORITY 1: SID Search (Exact Unique Record) ---
        if (StringUtils.hasText(criteria.getSid())) {
            String rawSid = criteria.getSid();
            // Filter by NameType='P' to ensure we get 1 row per SID
            return nameRepo.findBySidPrimary(rawSid, pageable)
                    .map(this::convertEntityToDTO);
        }

        // --- PRIORITY 2: FBI Search (Exact Unique Record) ---
        if (StringUtils.hasText(criteria.getFbiNumber())) {
            return nameRepo.findByFbiPrimary(
                            criteria.getFbiNumber().toUpperCase().trim(), pageable)
                    .map(this::convertEntityToDTO);
        }

        // --- PRIORITY 3: SSN Search (Exact Unique Record) ---
        if (StringUtils.hasText(criteria.getSsn())) {
            return nameRepo.findBySsnPrimary(
                            criteria.getSsn().trim(), pageable)
                    .map(this::convertEntityToDTO);
        }

        // --- PRIORITY 4: Driver's License Search ---
        if (StringUtils.hasText(criteria.getDlNumber())) {
            // Data is already pre-processed by Frontend (e.g. Num="620...", State="MDM")
            // Just ensure it's trimmed/uppercase for safety
            String dlNum = criteria.getDlNumber().trim().toUpperCase();
            String dlState = StringUtils.hasText(criteria.getDlState())
                    ? criteria.getDlState().trim().toUpperCase()
                    : null;

            return nameRepo.findByDlPrimary(dlNum, dlState, pageable)
                    .map(this::convertEntityToDTO);
        }

        // --- PRIORITY 5: Name / Soundex Search ---
        if (StringUtils.hasText(criteria.getFullName())) {

            // Parse Name (Last, First)
            NameParts parts = parseFullName(criteria.getFullName());

            // Prepare Partial First Name Pattern (e.g. "KEN%")
            // Mainframe Logic: Always filters by First Name prefix if provided
            String firstNamePattern = StringUtils.hasText(parts.first) ? parts.first + "%" : "";

            // Path A: SDX (Soundex)
            if ("SDX".equalsIgnoreCase(criteria.getTypeOfRequest())) {
                // Validation: SID/FBI/SSN must be empty for SDX
                if (StringUtils.hasText(criteria.getSid()) || StringUtils.hasText(criteria.getFbiNumber()) || StringUtils.hasText(criteria.getSsn())) {
                    throw new IllegalArgumentException("SID, FBI, and SSN must be empty for SDX Search.");
                }

                String soundex = utils.calculateStandardSoundex(parts.last);

                // Query: Soundex(Last) + Like(First) + Date Range
                return nameRepo.findBySoundex(
                                soundex, firstNamePattern, startDob, endDob, race, sex, pageable)
                        .map(this::convertEntityToDTO);
            }

            // Path B: Space (Standard)
            else {
                // Query: Exact(Last) + Like(First) + Date Range
                return nameRepo.findExactMatch(
                                parts.last, firstNamePattern, startDob, endDob, race, sex, pageable)
                        .map(this::convertEntityToDTO);
            }
        }

        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    // --- Helper: DTO Conversion ---
    private SearchResultDTO convertEntityToDTO(IdentName matchedName) {
        SearchResultDTO dto = new SearchResultDTO();
        IdentMaster master = matchedName.getMaster();

        dto.setSystemId(master.getSystemId());
        dto.setSidNumber(master.getSid());
        dto.setFbiNumber(master.getFbiNumber());

        String lName = matchedName.getLastName() != null ? matchedName.getLastName().trim() : "";
        String fName = matchedName.getFirstName() != null ? matchedName.getFirstName().trim() : "";
        String mName = matchedName.getMiddleName() != null ? matchedName.getMiddleName().trim() : "";

        // Logic: "LAST, FIRST MIDDLE"
        StringBuilder fullName = new StringBuilder(lName);

        if (!fName.isEmpty()) {
            fullName.append(", ").append(fName);
        }

        if (!mName.isEmpty()) {
            fullName.append(" ").append(mName);
        }

        dto.setFormattedName(fullName.toString());

        dto.setRace(matchedName.getRaceCode());
        dto.setSex(matchedName.getSexCode());
        dto.setDateOfBirth(matchedName.getDateOfBirth());

        // Fingerprint Pattern (Convert Raw -> Display)
        String rawFp = matchedName.getMafisFingerprint();
        String convertedFp = utils.convertMafisHandToDisplay(rawFp); // e.g. "AW\W\AW\W\"

        if (convertedFp != null && convertedFp.length() >= 10) {
            // Split: First 5 (Right) + SPACE + Next 5 (Left)
            String right = convertedFp.substring(0, 5);
            String left = convertedFp.substring(5, 10);
            dto.setPrintType(right + " " + left);
        } else {
            // Fallback if data is incomplete
            dto.setPrintType(convertedFp);
        }

        // Flag if this is an Alias (NameType != 'P')
        dto.setAliasMatch(!"P".equals(matchedName.getNameType()));

        return dto;
    }

    // --- Helper: Name Parsing ---
    private record NameParts(String last, String first) {}

    private NameParts parseFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) return new NameParts("", "");

        String cleaned = fullName.toUpperCase().trim();

        if (cleaned.contains(",")) {
            String[] parts = cleaned.split(",");
            String last = parts[0].trim();

            String firstFull = parts.length > 1 ? parts[1].trim() : "";

            String first = firstFull.split("\\s+")[0];

            return new NameParts(last, first);
        }

        return new NameParts(cleaned, "");
    }

    // --- Helper: Date Parsing (Range Support) ---
    private record DateRange(LocalDate start, LocalDate end) {}

    private DateRange parseDobRange(String dobString) {
        if (!StringUtils.hasText(dobString)) return null;

        // Try "MM/dd/yyyy" (Exact Date)
        try {
            LocalDate date = LocalDate.parse(dobString, DOB_FMT);
            return new DateRange(date, date); // Start = End
        } catch (Exception e) {
            // Continue to Year check
        }

        // Try "yyyy" (Year Only) - Common for SDX
        if (dobString.matches("^\\d{4}$")) {
            int year = Integer.parseInt(dobString);
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            return new DateRange(start, end);
        }

        return null;
    }
}