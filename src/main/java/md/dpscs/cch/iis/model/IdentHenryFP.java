package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "T_IDENT_FP_HENRY")
@Data
public class IdentHenryFP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HenryID")
    private Long henryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID", nullable = false)
    private IdentMaster master;

    @Column(name = "PrimaryHenry", length = 8)
    private String primaryHenry;

    @Column(name = "FpcHenry", length = 22)
    private String fpcHenry;
}