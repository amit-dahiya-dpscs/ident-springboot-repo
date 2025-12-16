package md.dpscs.cch.iis.controller;

import jakarta.servlet.http.HttpServletRequest;
import md.dpscs.cch.iis.dto.PersonDetailDTO;
import md.dpscs.cch.iis.dto.SearchCriteriaDTO;
import md.dpscs.cch.iis.dto.SearchResultDTO;
import md.dpscs.cch.iis.service.AuditService;
import md.dpscs.cch.iis.service.DetailService;
import md.dpscs.cch.iis.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/ident")
public class IdentIndexController {

    private final SearchService searchService;
    private final DetailService detailService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IdentIndexController(SearchService searchService, DetailService detailService, AuditService auditService) {
        this.searchService = searchService;
        this.detailService = detailService;
        this.auditService = auditService;
    }

    @PostMapping("/search")
    public ResponseEntity<Page<SearchResultDTO>> searchRecords(
            @Valid @RequestBody SearchCriteriaDTO criteria,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails, // Inject logged in user
            HttpServletRequest request) { // Inject request for IP

        // 1. Capture Audit Info
        String username = userDetails.getUsername();
        String ipAddress = request.getRemoteAddr();

        try {
            String criteriaJson = objectMapper.writeValueAsString(criteria);
            // 2. Call Async Audit (Non-blocking)
            auditService.logAction(username, ipAddress, "SEARCH_CLICK", criteriaJson);
        } catch (Exception e) {
            // Don't fail search if audit fails, but log it
        }

        // 3. Perform Search
        Page<SearchResultDTO> results = searchService.executeSearch(criteria, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{systemId}")
    public ResponseEntity<PersonDetailDTO> getDetailRecord(
            @PathVariable Long systemId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        String username = userDetails.getUsername();
        String ipAddress = request.getRemoteAddr();

        auditService.logAction(username, ipAddress, "VIEW_DETAIL", "SystemID: " + systemId);

        PersonDetailDTO details = detailService.getPersonDetails(systemId);
        return ResponseEntity.ok(details);
    }
}