package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.PersonName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonNameRepository extends JpaRepository<PersonName, PersonName.PersonNameId> {

    // Retrieve all names (primary + aliases) for the DetailService.
    List<PersonName> findAllById_PersonId(Long personId);

    //Explicitly relies on the legacy rule (InsertSeqNum = 1)
    // to retrieve the unique primary name record without relying on sorting/limiting.
    Optional<PersonName> findById_PersonIdAndId_InsertSeqNum(Long personId, Integer insertSeqNum);
}
