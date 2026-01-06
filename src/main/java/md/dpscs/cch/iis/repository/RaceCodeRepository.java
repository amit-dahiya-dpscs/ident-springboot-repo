package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.RaceCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaceCodeRepository extends JpaRepository<RaceCode, String> {
    List<RaceCode> findAllByOrderByCodeAsc();
}