package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.DocumentIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentIndexRepository extends JpaRepository<DocumentIndex, Long> {
    List<DocumentIndex> findAllByPersonIdOrderByDocumentDateDesc(Long personId);
}
