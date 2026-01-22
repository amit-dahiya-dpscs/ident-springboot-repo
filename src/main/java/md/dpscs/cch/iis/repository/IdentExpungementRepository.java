package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentExpungement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentExpungementRepository extends JpaRepository<IdentExpungement, Long> {
}