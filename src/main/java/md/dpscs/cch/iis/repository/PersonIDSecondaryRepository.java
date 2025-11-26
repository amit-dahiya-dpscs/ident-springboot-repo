package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.PersonIDSecondary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonIDSecondaryRepository extends JpaRepository<PersonIDSecondary, Long> {
    // Finds all secondary IDs (SSN, DL, Misc) for the DetailService
    List<PersonIDSecondary> findAllByPersonId(Long personId);
}
