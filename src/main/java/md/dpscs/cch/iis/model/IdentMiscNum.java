package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "T_IDENT_MISC_NUM")
@Data
public class IdentMiscNum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MiscID")
    private Long miscId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID", nullable = false)
    private IdentMaster master;

    @Column(name = "MiscNumType", nullable = false, length = 3)
    private String miscNumType;

    @Column(name = "MiscNumber", nullable = false, length = 12)
    private String miscNumber;
}