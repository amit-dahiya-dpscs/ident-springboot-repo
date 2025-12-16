package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentDL;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentDLRepository extends JpaRepository<IdentDL, Long> {
    List<IdentDL> findByMaster_SystemId(Long systemId);
}