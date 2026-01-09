package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "T_IDENT_NAMES")
@Data
public class IdentName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NameID")
    private Long nameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID", nullable = false)
    private IdentMaster master;

    @Column(name = "NameType")
    private String nameType; // 'P', 'A'

    @Column(name = "LastName", nullable = false)
    private String lastName;

    @Column(name = "FirstName", nullable = false)
    private String firstName;

    @Column(name = "MiddleInitial")
    private String middleInitial;

    @Column(name = "MiddleName")
    private String middleName;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "RaceCode")
    private String raceCode;

    @Column(name = "SexCode")
    private String sexCode;

    @Column(name = "MafisFingerprint")
    private String mafisFingerprint;

    @Column(name = "SoundexCode")
    private String soundexCode;

    @Column(name = "SequenceNumber")
    private Integer sequenceNumber;
}