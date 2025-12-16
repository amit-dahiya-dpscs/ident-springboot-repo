package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentFlagRepository extends JpaRepository<IdentFlag, Long> {
    List<IdentFlag> findByMaster_SystemId(Long systemId);
}