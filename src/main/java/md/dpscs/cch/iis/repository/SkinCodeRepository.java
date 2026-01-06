package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.SkinCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkinCodeRepository extends JpaRepository<SkinCode, String> {
    List<SkinCode> findAllByOrderByCodeAsc();
}