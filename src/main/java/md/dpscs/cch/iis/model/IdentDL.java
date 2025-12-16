package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "T_IDENT_DL")
@Data
public class IdentDL {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DlID")
    private Long driverLicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID", nullable = false)
    private IdentMaster master;

    @Column(name = "LicenseNumber", nullable = false, length = 22)
    private String licenseNumber;

    @Column(name = "StateSource", length = 3)
    private String stateSource;
}