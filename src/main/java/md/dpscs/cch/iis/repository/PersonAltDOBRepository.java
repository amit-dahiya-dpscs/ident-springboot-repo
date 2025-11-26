package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.PersonAltDOB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonAltDOBRepository extends JpaRepository<PersonAltDOB, Long> {
    // Finds all alternate DOBs for the DetailService
    List<PersonAltDOB> findAllByPersonId(Long personId);
}
