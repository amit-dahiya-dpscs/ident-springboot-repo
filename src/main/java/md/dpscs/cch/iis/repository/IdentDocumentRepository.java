package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IdentDocumentRepository extends JpaRepository<IdentDocument, Long> {

    List<IdentDocument> findByMaster_SystemIdOrderByDocumentDateDesc(Long systemId);

    boolean existsByMaster_SystemIdAndDocumentTypeAndDocumentNumberAndDocumentDate(
            Long systemId, String documentType, String documentNumber, LocalDate documentDate
    );

    List<IdentDocument> findByMaster_SystemId(Long systemId);

    long countByMaster_SystemIdAndDocumentTypeIn(Long systemId, List<String> documentTypes);

    long countByMaster_SystemId(Long systemId);

    void deleteAllByMaster_SystemId(Long systemId);
}