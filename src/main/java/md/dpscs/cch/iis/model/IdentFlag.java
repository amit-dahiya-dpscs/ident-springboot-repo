package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "T_IDENT_FLAGS")
@Data
public class IdentFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FlagID")
    private Long flagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID")
    private IdentMaster master;

    @Column(name = "FlagType")
    private String flagType; // 'CAUTION', 'STATUS'

    @Column(name = "FlagCode")
    private String flagCode;
}