package md.dpscs.cch.iis.controller;

import md.dpscs.cch.iis.dto.PersonDetailDTO;
import md.dpscs.cch.iis.dto.SearchCriteriaDTO;
import md.dpscs.cch.iis.dto.SearchResultDTO;
import md.dpscs.cch.iis.service.DetailService;
import md.dpscs.cch.iis.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ident")
public class IdentIndexController {

    private final SearchService searchService;
    private final DetailService detailService;

    public IdentIndexController(SearchService searchService, DetailService detailService) {
        this.searchService = searchService;
        this.detailService = detailService;
    }

    @PostMapping("/search")
    public ResponseEntity<Page<SearchResultDTO>> searchRecords(
            @Valid @RequestBody SearchCriteriaDTO criteria,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<SearchResultDTO> results = searchService.executeSearch(criteria, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{systemId}")
    public ResponseEntity<PersonDetailDTO> getDetailRecord(@PathVariable Long systemId) {
        PersonDetailDTO details = detailService.getPersonDetails(systemId);
        return ResponseEntity.ok(details);
    }
}