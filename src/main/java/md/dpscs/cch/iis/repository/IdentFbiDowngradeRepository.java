package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentFbiDowngrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentFbiDowngradeRepository extends JpaRepository<IdentFbiDowngrade, Long> {

    Optional<IdentFbiDowngrade> findBySidAndFbiNumberAndFbiRecordIndicator(Long systemId, String sid, String fbiNumber, String ind);

}
