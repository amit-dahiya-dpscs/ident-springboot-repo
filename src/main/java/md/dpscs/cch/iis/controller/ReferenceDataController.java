package md.dpscs.cch.iis.controller;

import md.dpscs.cch.iis.model.CountryCode;
import md.dpscs.cch.iis.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reference")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @GetMapping("/countries")
    public ResponseEntity<List<CountryCode>> getCountries() {
        return ResponseEntity.ok(referenceDataService.getAllCountryCodes());
    }

    @GetMapping("/cautions")
    public ResponseEntity<Map<String, String>> getCautions() {
        return ResponseEntity.ok(referenceDataService.getAllCautionCodes());
    }
}