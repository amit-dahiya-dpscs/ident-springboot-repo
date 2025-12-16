package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.CautionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CautionCodeRepository extends JpaRepository<CautionCode, String> {
    List<CautionCode> findAllByOrderByCodeAsc();
}