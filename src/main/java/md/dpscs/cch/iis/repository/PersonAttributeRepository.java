package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.PersonAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonAttributeRepository extends JpaRepository<PersonAttribute, Long> {
    // Finds all caution/scar entries for the person (DetailService aggregation)
    List<PersonAttribute> findAllByPersonId(Long personId);
}
