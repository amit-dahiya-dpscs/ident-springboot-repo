package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "T_IDENT_MASTER")
@Getter
@Setter
public class IdentMaster {
    @Id
    @Column(name = "SystemID")
    private Long systemId;

    @Column(name = "SID", nullable = false)
    private String sid;

    @Column(name = "FBINumber")
    private String fbiNumber;

    @Column(name = "RecordType")
    private String recordType;

    @Column(name = "LastUpdateDate")
    private LocalDateTime lastUpdateDate;

    // --- Demographics ---
    @Column(name = "Height")
    private String height;
    @Column(name = "Weight")
    private String weight;
    @Column(name = "RaceCode")
    private String raceCode;
    @Column(name = "SexCode")
    private String sexCode;
    @Column(name = "EyeColorCode")
    private String eyeColorCode;
    @Column(name = "HairColorCode")
    private String hairColorCode;
    @Column(name = "SkinToneCode")
    private String skinToneCode;
    @Column(name = "PlaceOfBirthCode")
    private String placeOfBirthCode;
    @Column(name = "CitizenshipCode")
    private String citizenshipCode;

    // --- Flags ---
    @Column(name = "MugshotFlag")
    private String mugshotFlag;
    @Column(name = "DnaFlag")
    private String dnaFlag;
    @Column(name = "DisseminationFlag")
    private String disseminationFlag;
    @Column(name = "IIIFlag", length = 1)
    private String iiiStatus;
    @Column(name = "RapbackFlag", length = 1)
    private String rapbackSubscriptionIndicator;
    @Column(name = "Comments")
    private String comments;

    // --- CRITICAL RELATIONSHIPS FOR SEARCH JOINS ---

    // Required for findBySsnPrimary ("JOIN m.ssnList s")
    @OneToMany(mappedBy = "master", fetch = FetchType.LAZY)
    private List<IdentSSN> ssnList;

    // Optional: Good practice to have these for future queries
    @OneToMany(mappedBy = "master", fetch = FetchType.LAZY)
    private List<IdentName> names;

    @OneToMany(mappedBy = "master", fetch = FetchType.LAZY)
    private List<IdentDL> driverLicenses;
}