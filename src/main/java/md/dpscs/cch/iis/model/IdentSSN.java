package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_IDENT_SSN")
@Data
public class IdentSSN {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SsnID")
    private Long ssnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID", nullable = false)
    private IdentMaster master;

    @Column(name = "SSN", nullable = false, length = 9)
    private String ssn;

    @Column(name = "DateRecorded")
    private LocalDateTime dateRecorded;
}