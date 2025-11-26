package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    // --- Search Routing Methods (II0200C Priority 1 & 2) ---
    List<Person> findAllByStateId(String stateId);
    List<Person> findAllByFbiNumber(String fbiNumber);

    // --- Complex Search Method (II0200C Priority 3: Name/Soundex) ---
    // NOTE: This uses an @Query to join Person and PersonName tables based on SoundexCode.
    @Query("SELECT p FROM Person p JOIN PersonName pn ON p.personId = pn.id.personId WHERE pn.soundexCode = :soundexCode")
    List<Person> findAllBySoundexCode(@Param("soundexCode") String soundexCode);

    // --- Demographic Acceptance Tests (Filtering) ---
    List<Person> findAllByRaceCodeAndSexCode(String raceCode, String sexCode);
}
