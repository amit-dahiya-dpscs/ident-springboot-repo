package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentScarsMarks;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IdentScarsMarksRepository extends JpaRepository<IdentScarsMarks, Long> {
    List<IdentScarsMarks> findByMaster_SystemId(Long systemId);
    void deleteByMaster_SystemId(Long systemId);
}