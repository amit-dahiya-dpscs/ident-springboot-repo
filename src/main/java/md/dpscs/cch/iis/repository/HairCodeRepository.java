package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.HairCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HairCodeRepository extends JpaRepository<HairCode, String> {
    List<HairCode> findAllByOrderByCodeAsc();
}