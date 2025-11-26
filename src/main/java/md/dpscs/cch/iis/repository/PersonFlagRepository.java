package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.PersonFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonFlagRepository extends JpaRepository<PersonFlag, Long> {
    // Used by DetailService to retrieve all normalized flags (replaces checking 13 columns)
    List<PersonFlag> findByPersonId(Long personId);
}
