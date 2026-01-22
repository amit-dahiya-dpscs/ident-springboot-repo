package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "T_IDENT_EXPUNGEMENT") // Consistent with T_IDENT_MASTER
public class IdentExpungement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXPUNGEMENT_ID")
    private Long id;

    // --- Core Identifiers ---
    @Column(name = "SID", length = 10, nullable = false)
    private String sid;

    @Column(name = "FBI_NUMBER", length = 9)
    private String fbiNumber;

    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    // --- Transaction Details ---
    @Column(name = "PROCESS_TYPE", length = 3, nullable = false)
    private String processType; // e.g., 'DWN', 'CXL', 'PAR'

    @CreationTimestamp
    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDateTime processDate;

    // --- Legacy Input Fields (From React Modal) ---
    @Column(name = "COGENT_PCN", length = 15)
    private String cogentPcn;

    @Column(name = "COGENT_PCN_2", length = 15)
    private String cogentPcn2;

    @Column(name = "COURT_CASE_NUMBER", length = 20)
    private String courtCaseNumber;

    @Column(name = "CHARGE_DESCRIPTION", length = 80)
    private String chargeDescription;

    @Column(name = "REASON_FOR_DELETION", length = 50)
    private String reasonForDeletion;

    // --- Snapshot Fields (Legacy Parity) ---
    @Column(name = "SNAPSHOT_LAST_NAME", length = 29)
    private String lastName;

    @Column(name = "SNAPSHOT_FIRST_NAME", length = 27)
    private String firstName;

    @Column(name = "EVENT_DATE", length = 26)
    private String eventDate;

    @Column(name = "FBI_EXP_INDICATOR", length = 1)
    private String fbiExpungementIndicator;
}