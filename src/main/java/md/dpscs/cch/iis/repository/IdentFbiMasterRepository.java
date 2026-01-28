package md.dpscs.cch.iis.repository;

import md.dpscs.cch.iis.model.IdentFbiMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentFbiMasterRepository extends JpaRepository<IdentFbiMaster, String> {
    // Matches Mainframe Logic: SELECT COUNT(*) ... WHERE FT_SID = :SID
    boolean existsBySid(String sid);
}
