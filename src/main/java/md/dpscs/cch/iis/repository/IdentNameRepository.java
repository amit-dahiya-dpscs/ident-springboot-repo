package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface IdentNameRepository extends JpaRepository<IdentName, Long> {

    List<IdentName> findByMaster_SystemId(Long systemId);

    @Query("SELECT COALESCE(MAX(n.sequenceNumber), 0) FROM IdentName n WHERE n.master.systemId = :systemId")
    Integer findMaxSequenceBySystemId(@Param("systemId") Long systemId);

    boolean existsByMaster_SystemIdAndLastNameAndFirstNameAndMiddleNameAndNameType(
            Long systemId, String lastName, String firstName, String middleName, String nameType
    );

    @Query("SELECT n FROM IdentName n JOIN n.master m WHERE m.sid = :sid AND n.nameType = 'P'")
    Page<IdentName> findBySidPrimary(@Param("sid") String sid, Pageable pageable);

    @Query("SELECT n FROM IdentName n JOIN n.master m WHERE m.fbiNumber = :fbi AND n.nameType = 'P'")
    Page<IdentName> findByFbiPrimary(@Param("fbi") String fbi, Pageable pageable);

    @Query("SELECT n FROM IdentName n JOIN n.master m JOIN m.ssnList s WHERE s.ssn = :ssn AND n.nameType = 'P'")
    Page<IdentName> findBySsnPrimary(@Param("ssn") String ssn, Pageable pageable);

    // --- Driver's License Search ---
    @Query("SELECT n FROM IdentName n " +
            "JOIN n.master m " +
            "JOIN m.driverLicenses d " +
            "WHERE d.licenseNumber = :dlNum " +
            "AND (:dlState IS NULL OR d.stateSource = :dlState) " + // EXACT MATCH (=)
            "AND n.nameType = 'P'")
    Page<IdentName> findByDlPrimary(
            @Param("dlNum") String dlNum,
            @Param("dlState") String dlState,
            Pageable pageable);

    @Query("SELECT n FROM IdentName n WHERE " +
            "n.lastName = :lastName " +
            "AND (:firstName = '' OR n.firstName LIKE :firstName) " +
            "AND (:dobStart IS NULL OR n.dateOfBirth >= :dobStart) " +
            "AND (:dobEnd IS NULL OR n.dateOfBirth <= :dobEnd) " +
            "AND (:race IS NULL OR n.raceCode = :race) " +
            "AND (:sex IS NULL OR n.sexCode = :sex)")
    Page<IdentName> findExactMatch(
            @Param("lastName") String lastName,
            @Param("firstName") String firstName,
            @Param("dobStart") LocalDate dobStart,
            @Param("dobEnd") LocalDate dobEnd,
            @Param("race") String race,
            @Param("sex") String sex,
            Pageable pageable);

    @Query("SELECT n FROM IdentName n WHERE " +
            "n.soundexCode = :soundex " +
            "AND (:firstName = '' OR n.firstName LIKE :firstName%) " +
            "AND (:dobStart IS NULL OR n.dateOfBirth >= :dobStart) " +
            "AND (:dobEnd IS NULL OR n.dateOfBirth <= :dobEnd) " +
            "AND (:race IS NULL OR n.raceCode = :race) " +
            "AND (:sex IS NULL OR n.sexCode = :sex)")
    Page<IdentName> findBySoundex(
            @Param("soundex") String soundex,
            @Param("firstName") String firstName,
            @Param("dobStart") LocalDate dobStart,
            @Param("dobEnd") LocalDate dobEnd,
            @Param("race") String race,
            @Param("sex") String sex,
            Pageable pageable);
}