package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_IDENT_SCARS_MARKS")
@Data
public class IdentScarsMarks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ScarID")
    private Long scarId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID", nullable = false)
    private IdentMaster master;

    @Column(name = "Code", nullable = false, length = 10)
    private String code;

    @Column(name = "Description", length = 50)
    private String description;

    @Column(name = "CreateTimestamp")
    private LocalDateTime createTimestamp;
}