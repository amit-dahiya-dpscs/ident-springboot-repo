package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "T_IDENT_ADDRESS")
@Data
public class IdentAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AddressID")
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID")
    private IdentMaster master;

    @Column(name = "StreetNumber") private String streetNumber;
    @Column(name = "StreetDirection") private String streetDirection;
    @Column(name = "StreetName") private String streetName;
    @Column(name = "StreetSuffix") private String streetSuffix;
    @Column(name = "City") private String city;
    @Column(name = "StateCode") private String stateCode;
    @Column(name = "ZipCode") private String zipCode;
    @Column(name = "IsCurrent") private Boolean isCurrent;
}