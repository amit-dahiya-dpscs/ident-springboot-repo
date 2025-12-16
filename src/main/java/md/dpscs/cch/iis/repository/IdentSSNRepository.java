package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentSSN;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentSSNRepository extends JpaRepository<IdentSSN, Long> {
    List<IdentSSN> findByMaster_SystemId(Long systemId);
}