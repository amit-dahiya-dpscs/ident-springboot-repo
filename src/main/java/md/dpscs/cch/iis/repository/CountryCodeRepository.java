package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.CountryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CountryCodeRepository extends JpaRepository<CountryCode, String> {
    List<CountryCode> findAllByOrderByDescriptionAsc();
}