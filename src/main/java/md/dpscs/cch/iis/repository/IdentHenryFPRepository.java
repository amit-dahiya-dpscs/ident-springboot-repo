package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentHenryFP;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentHenryFPRepository extends JpaRepository<IdentHenryFP, Long> {
    List<IdentHenryFP> findByMaster_SystemId(Long systemId);
}