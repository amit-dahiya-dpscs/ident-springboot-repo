package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "T_IDENT_FP_NCIC")
@Data
public class IdentNcicFP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NcicID")
    private Long ncicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID", nullable = false)
    private IdentMaster master;

    @Column(name = "FpcPattern", length = 20)
    private String fpcPattern;
}