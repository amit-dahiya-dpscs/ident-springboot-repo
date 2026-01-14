package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.SmtCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SmtCodeRepository extends JpaRepository<SmtCode, String> {
    // Used by ReferenceDataService to cache all codes
    List<SmtCode> findAllByOrderByCodeAsc();
}