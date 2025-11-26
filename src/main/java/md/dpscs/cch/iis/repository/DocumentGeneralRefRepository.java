package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.DocumentGeneralRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentGeneralRefRepository extends JpaRepository<DocumentGeneralRef, Long> {
    List<DocumentGeneralRef> findAllByPersonIdOrderByDocumentDateDesc(Long personId);
}