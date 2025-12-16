package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.model.CautionCode;
import md.dpscs.cch.iis.model.CountryCode;
import md.dpscs.cch.iis.repository.CautionCodeRepository;
import md.dpscs.cch.iis.repository.CountryCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final CountryCodeRepository countryRepo;
    private final CautionCodeRepository cautionRepo;

    @Transactional(readOnly = true)
    public List<CountryCode> getAllCountryCodes() {
        return countryRepo.findAllByOrderByDescriptionAsc();
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAllCautionCodes() {
        // Convert List<Entity> to Map<Code, Description> for the Frontend
        return cautionRepo.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(
                        CautionCode::getCode,
                        CautionCode::getDescription,
                        (existing, replacement) -> existing // Merge function (keep existing)
                ));
    }
}