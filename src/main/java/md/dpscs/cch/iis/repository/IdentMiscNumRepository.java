package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentMiscNum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentMiscNumRepository extends JpaRepository<IdentMiscNum, Long> {
    List<IdentMiscNum> findByMaster_SystemId(Long systemId);
    void deleteByMaster_SystemId(Long systemId);
}