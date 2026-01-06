package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.SexCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SexCodeRepository extends JpaRepository<SexCode, String> {
    List<SexCode> findAllByOrderByCodeAsc();
}