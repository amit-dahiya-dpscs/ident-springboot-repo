package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentNcicFP;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentNcicFPRepository extends JpaRepository<IdentNcicFP, Long> {
    List<IdentNcicFP> findByMaster_SystemId(Long systemId);
}