package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps to Legacy Table: AD.ADT_FBDGR
 * Purpose: Logs actions for FBI-Owned records. Replaces standard DRS notification.
 */
@Entity
@Data
@Table(name = "T_IDENT_FBI_DOWNGRADE")
public class IdentFbiDowngrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DGR_ID")
    private Long id;

    // --- Identifiers ---
    @Column(name = "DGR_SID", length = 10, nullable = false)
    private String sid;

    @Column(name = "DGR_FBINO", length = 9)
    private String fbiNumber;

    @Column(name = "DGR_SYSNUM", length = 11)
    private Long systemId;

    @Column(name = "DGR_SSN", length = 9)
    private String ssn;

    // --- Demographics Snapshot ---
    @Column(name = "DGR_LNAME", length = 29)
    private String lastName;

    @Column(name = "DGR_FNAME", length = 27)
    private String firstName;

    @Column(name = "DGR_MNAME", length = 27)
    private String middleName;

    @Column(name = "DGR_DOB", length = 10)
    private String dob; // Format: YYYY-MM-DD

    @Column(name = "DGR_ARRDT")
    private LocalDate arrestDate;

    @Column(name = "DGR_PCN", length = 15)
    private String pcn;

    @Column(name = "DGR_COURT_CASE", length = 20)
    private String courtCase;

    @Column(name = "DGR_CHRG_DESC", length = 80)
    private String chargeDescription;

    @Column(name = "DGR_USER", length = 8)
    private String userId;

    @Column(name = "DGR_FBIREC", length = 1)
    private String fbiRecordIndicator; // 'Y' indicates FBI ownership confirmed

    @CreationTimestamp
    @Column(name = "DGR_DTE_TME")
    private LocalDateTime processTimestamp;
}