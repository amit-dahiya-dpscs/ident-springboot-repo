package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentDobAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentDobAliasRepository extends JpaRepository<IdentDobAlias, Long> {
    List<IdentDobAlias> findByMaster_SystemId(Long systemId);
}