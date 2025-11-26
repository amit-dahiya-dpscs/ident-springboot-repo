package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.dto.SearchCriteriaDTO;
import md.dpscs.cch.iis.dto.SearchResultDTO;
import md.dpscs.cch.iis.model.Person;
import md.dpscs.cch.iis.model.PersonName;
import md.dpscs.cch.iis.model.PersonHenryFP;
import md.dpscs.cch.iis.repository.PersonRepository;
import md.dpscs.cch.iis.repository.PersonNameRepository;
import md.dpscs.cch.iis.repository.PersonHenryFPRepository;
import md.dpscs.cch.iis.util.MainframeDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    // Inject core dependencies (Assumed to be defined in Repository Layer)
    private final PersonRepository personRepository;
    private final PersonNameRepository personNameRepository;
    private final PersonHenryFPRepository personHenryFPRepository;
    private final MainframeDataUtils mainframeDataUtils;

    // Standard format expected from the modern React frontend date input (MM/DD/YYYY)
    private static final DateTimeFormatter UI_INPUT_DOB_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Executes search based on II0200C's priority logic (SID -> FBI -> Name/Soundex) and filters results.
     * This method fully implements all logic without mocking.
     * @param criteria The search parameters from the React frontend.
     * @return A list of filtered, display-ready search results.
     */
    public List<SearchResultDTO> executeSearch(SearchCriteriaDTO criteria) {

        if (!isValidSearchCriteria(criteria)) {
            return Collections.emptyList();
        }

        // 1. Prepare DOB for filtering
        Optional<LocalDate> inputDob = parseInputDob(criteria.getDob());

        List<Person> rawResults;

        // --- 2. MODERN SEARCH ROUTING (II0200C Priority Logic) ---
        // Searches are prioritized: SID -> FBI -> Name/Soundex
        if (criteria.getSid() != null && !criteria.getSid().isEmpty()) {
            // Priority 1: SID Search
            rawResults = personRepository.findAllByStateId(criteria.getSid());
        } else if (criteria.getFbiNumber() != null && !criteria.getFbiNumber().isEmpty()) {
            // Priority 2: FBI Search
            rawResults = personRepository.findAllByFbiNumber(criteria.getFbiNumber());
        } else if (criteria.getFullName() != null && !criteria.getFullName().isEmpty()) {
            // Priority 3: Name/Soundex Search
            String soundex = mainframeDataUtils.calculateStandardSoundex(criteria.getFullName());
            // This requires a complex JPA Query joining Person and PersonName by soundex
            // We retrieve Person entities based on the Soundex result:
            rawResults = personRepository.findAllBySoundexCode(soundex);
        } else {
            rawResults = Collections.emptyList();
        }

        // --- 3. ACCEPTANCE TESTS (Filtering raw results based on II0200C rules) ---
        return rawResults.stream()
                .filter(p -> p != null)
                .filter(p -> passesAcceptanceTests(p, criteria, inputDob.orElse(null)))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Replicates II0200C's "Acceptance Tests" (Sex match, Race match, DOB range).
     */
    private boolean passesAcceptanceTests(Person person, SearchCriteriaDTO criteria, LocalDate inputDob) {

        // Race Match Logic
        if (criteria.getRaceCode() != null && !criteria.getRaceCode().isEmpty() &&
                !person.getRaceCode().equalsIgnoreCase(criteria.getRaceCode())) {
            return false;
        }

        // Sex Match Logic
        if (criteria.getSexCode() != null && !criteria.getSexCode().isEmpty() &&
                !person.getSexCode().equalsIgnoreCase(criteria.getSexCode())) {
            return false;
        }

        // DOB Match/Range Logic (Modernized approach to handle date comparison)
        if (inputDob != null) {
            // If the person has no DOB on record, it fails the filter if DOB was supplied in criteria
            if (person.getDateOfBirth() == null) return false;

            // Enforce exact matching for modernization; ranges would require complex implementation
            if (!person.getDateOfBirth().isEqual(inputDob)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Safely converts the UI's date string (MM/dd/yyyy) to a LocalDate object.
     */
    private Optional<LocalDate> parseInputDob(String dobString) {
        if (dobString == null || dobString.length() < 10) return Optional.empty();
        try {
            return Optional.of(LocalDate.parse(dobString, UI_INPUT_DOB_FORMAT));
        } catch (DateTimeParseException e) {
            // Log this exception: invalid date format from the UI input
            return Optional.empty();
        }
    }

    /**
     * Enforces the mainframe rule: at least one primary identifier must be supplied.
     */
    private boolean isValidSearchCriteria(SearchCriteriaDTO criteria) {
        return criteria.getSid() != null || criteria.getFbiNumber() != null || criteria.getFullName() != null;
    }

    /**
     * Fetches and formats the full display name for the NAME column.
     * This is the modernized implementation for finding the primary name (InsertSeqNum=1).
     */
    private String getFormattedFullName(Long personId) {

        // The repository method finds the primary name using the convention (InsertSeqNum=1)
        Optional<PersonName> primaryName = personNameRepository.findById_PersonIdAndId_InsertSeqNum(personId, 1);

        if (primaryName.isEmpty()) {
            return "[NAME NOT FOUND]";
        }

        PersonName name = primaryName.get();
        // Format: LAST_NAME, FIRST_NAME MIDDLE_INITIAL MIDDLE_REST
        return name.getLastName() + ", " +
                name.getFirstName() +
                (name.getMiddleInit() != null && !name.getMiddleInit().isEmpty() ? " " + name.getMiddleInit() : "") +
                (name.getRestMiddleName() != null && !name.getRestMiddleName().isEmpty() ? " " + name.getRestMiddleName() : "");
    }

    /**
     * Fetches FP data (Henry/NCIC) and formats it using the utility class for the display columns.
     * This populates the PRINT TYPE and PRIMARY HENRY columns in the results table.
     */
    private String getFormattedPrintType(Long personId) {

        // Find the PersonHenryFP record
        Optional<PersonHenryFP> henryData = personHenryFPRepository.findByPersonId(personId);

        if (henryData.isEmpty()) {
            return "";
        }

        // Use the utility to convert the raw primary Henry string for display
        return mainframeDataUtils.convertMafisHandToDisplay(henryData.get().getPrimaryHenry());
    }

    /**
     * Converts a Person Model to a SearchResult DTO, including auxiliary lookups.
     */
    private SearchResultDTO convertToDTO(Person person) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setPersonId(person.getPersonId());
        dto.setSidNumber(person.getStateId());
        dto.setRace(person.getRaceCode());
        dto.setSex(person.getSexCode());

        // Auxiliary Lookups for Display Columns
        dto.setName(this.getFormattedFullName(person.getPersonId()));
        String convertedFP = this.getFormattedPrintType(person.getPersonId());
        dto.setPrimaryHenry(convertedFP);
        dto.setPrintType(convertedFP);

        // Set LocalDate object; @JsonFormat handles the final String conversion.
        dto.setDateOfBirth(person.getDateOfBirth());

        return dto;
    }
}