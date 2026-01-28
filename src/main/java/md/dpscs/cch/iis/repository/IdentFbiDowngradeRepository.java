package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentFbiDowngrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentFbiDowngradeRepository extends JpaRepository<IdentFbiDowngrade, Long> {

}
