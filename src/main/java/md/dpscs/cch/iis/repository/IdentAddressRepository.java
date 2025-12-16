package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentAddressRepository extends JpaRepository<IdentAddress, Long> {
    List<IdentAddress> findByMaster_SystemId(Long systemId);
}