package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentDocumentRepository extends JpaRepository<IdentDocument, Long> {
    List<IdentDocument> findByMaster_SystemIdOrderByDocumentDateDesc(Long systemId);
}