package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.DocumentArrest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentArrestRepository extends JpaRepository<DocumentArrest, Long> {
    // Order By logic (DocumentDate DESC) mirrors the COBOL file processing order
    List<DocumentArrest> findAllByPersonIdOrderByDocumentDateDesc(Long personId);
}
