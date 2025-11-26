package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.PersonHenryFP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonHenryFPRepository extends JpaRepository<PersonHenryFP, Long> {
    Optional<PersonHenryFP> findByPersonId(Long personId);
}
