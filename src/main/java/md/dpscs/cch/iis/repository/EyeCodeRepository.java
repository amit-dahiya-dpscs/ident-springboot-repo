package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.EyeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EyeCodeRepository extends JpaRepository<EyeCode, String> {
    List<EyeCode> findAllByOrderByCodeAsc();
}