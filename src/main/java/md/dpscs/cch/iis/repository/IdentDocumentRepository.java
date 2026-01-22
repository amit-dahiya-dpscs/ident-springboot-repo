package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IdentDocumentRepository extends JpaRepository<IdentDocument, Long> {

    List<IdentDocument> findByMaster_SystemIdOrderByDocumentDateDesc(Long systemId);

    void deleteByMaster_SystemId(Long systemId);

    boolean existsByMaster_SystemIdAndDocumentTypeAndDocumentNumber(Long systemId, String documentType, String documentNumber);

    List<IdentDocument> findByMaster_SystemId(Long systemId);

    @Query("SELECT COUNT(d) FROM IdentDocument d WHERE d.master.systemId = :systemId AND d.documentType IN :criminalTypes")
    long countCriminalRecords(@Param("systemId") Long systemId, @Param("criminalTypes") List<String> criminalTypes);

    @Query("SELECT COUNT(d) FROM IdentDocument d WHERE d.master.systemId = :systemId AND d.documentType NOT IN :criminalTypes")
    long countNonCriminalRecords(@Param("systemId") Long systemId, @Param("criminalTypes") List<String> criminalTypes);
}