package md.dpscs.cch.iis.controller;

import md.dpscs.cch.iis.dto.PersonDetailDTO;
import md.dpscs.cch.iis.dto.SearchCriteriaDTO;
import md.dpscs.cch.iis.dto.SearchResultDTO;
import md.dpscs.cch.iis.service.DetailService;
import md.dpscs.cch.iis.service.SearchService;
import md.dpscs.cch.iis.service.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles all REST API entry points for the Ident Index Inquiry system.
 * Translates HTTP requests into calls to the core business logic services.
 */
@RestController
@RequestMapping("/api/ident")
@RequiredArgsConstructor // Lombok handles dependency injection for final fields
public class IdentIndexController {

    private final SearchService searchService;
    private final DetailService detailService;

    /**
     * POST /api/ident/search
     * Executes the II0200C search routing and filtering logic.
     * @param criteria The search parameters from the React frontend.
     * @return A list of filtered, display-ready search results.
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResultDTO>> searchRecords(@RequestBody SearchCriteriaDTO criteria) {
        // Validation for specific search types (e.g., proper length check) should occur here or in a pre-processor/filter

        List<SearchResultDTO> results = searchService.executeSearch(criteria);

        // Returns 200 OK with the list of search results for the results table.
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/ident/{personId}
     * Executes the II0400C detail aggregation logic to fetch all 13 data segments.
     * @param personId The unique ID of the person to view.
     * @return The complete PersonDetailDTO.
     */
    @GetMapping("/{personId}")
    public ResponseEntity<PersonDetailDTO> getDetailRecord(@PathVariable Long personId) {

        // Basic input validation: Checks for null/zero/negative ID.
        if (personId == null || personId <= 0) {
            return ResponseEntity.badRequest().body(null);
        }

        // Service call to aggregate data from all 13 tables.
        // If the resource is not found in the DB, the service throws ResourceNotFoundException.
        PersonDetailDTO details = detailService.getPersonDetails(personId);

        return ResponseEntity.ok(details);
    }

    /**
     * PUT /api/ident/{personId}
     * Placeholder for the update/modify logic (II0100C).
     * This endpoint handles the 'YES' command from the React UI.
     */
    @PutMapping("/{personId}")
    public ResponseEntity<Void> updateDetailRecord(@PathVariable Long personId, @RequestBody PersonDetailDTO updatedData) {
        // NOTE: Full implementation would call an UpdateService for II0100C logic
        // updateService.processUpdate(personId, updatedData);

        // Returns 204 No Content for a successful update (HTTP standard for PUT/DELETE success with no body)
        return ResponseEntity.noContent().build();
    }

    /**
     * Standardized Exception Handler to catch ResourceNotFoundException
     * thrown by the service layer and translate it into an HTTP 404 response.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        // Log the exception details here if necessary
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
