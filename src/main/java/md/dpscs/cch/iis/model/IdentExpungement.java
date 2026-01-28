package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps to Legacy Table: IP.IPT_RWEXP
 * Purpose: Queue for asynchronous IP07 transaction (TC/ETS/FBI Notification).
 */
@Entity
@Data
@Table(name = "T_IDENT_EXPUNGEMENT")
public class IdentExpungement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXPUNGEMENT_ID")
    private Long id;

    // --- Core Transaction Control ---
    @Column(name = "SID", length = 10, nullable = false)
    private String sid;

    @Column(name = "SYSTEM_ID", nullable = false)
    private Long systemId;

    @Column(name = "PROCESS_TYPE", length = 3, nullable = false)
    private String processType; // 'DWN', 'EXP', 'PAR', 'CXL'

    @Column(name = "FBI_EXP_INDICATOR", length = 1)
    private String fbiExpIndicator; // 'D', 'E', 'C'

    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    @CreationTimestamp
    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDateTime processDate;

    // --- Critical Identifiers ---
    @Column(name = "FBI_NUMBER", length = 9)
    private String fbiNumber;

    @Column(name = "SSN", length = 9)
    private String ssn;

    @Column(name = "TRACKING_NUMBER", length = 12)
    private String trackingNumber;

    @Column(name = "OCA_NUMBER", length = 20)
    private String ocaNumber;

    // --- Input Fields ---
    @Column(name = "COGENT_PCN", length = 15)
    private String cogentPcn;

    @Column(name = "COGENT_PCN_2", length = 15)
    private String cogentPcn2;

    @Column(name = "COURT_CASE_NUMBER", length = 20)
    private String courtCaseNumber;

    @Column(name = "CHARGE_DESCRIPTION", length = 80)
    private String chargeDescription;

    @Column(name = "REASON_FOR_DELETION", length = 50)
    private String reasonForDeletion;

    // --- Demographic Snapshot ---
    @Column(name = "LAST_NAME", length = 29)
    private String lastName;

    @Column(name = "FIRST_NAME", length = 27)
    private String firstName;

    @Column(name = "MIDDLE_NAME", length = 27)
    private String middleName;

    @Column(name = "DOB", length = 10)
    private String dob;

    @Column(name = "RACE_CODE", length = 1)
    private String raceCode;

    @Column(name = "SEX_CODE", length = 1)
    private String sexCode;

    @Column(name = "HEIGHT", length = 3)
    private String height;

    @Column(name = "WEIGHT", length = 3)
    private String weight;

    @Column(name = "EYE_CODE", length = 3)
    private String eyeCode;

    @Column(name = "HAIR_CODE", length = 3)
    private String hairCode;

    @Column(name = "SKIN_TONE_CODE", length = 3)
    private String skinToneCode;

    @Column(name = "POB_CODE", length = 2)
    private String pobCode;

    // --- Address Snapshot ---
    @Column(name = "STREET_NUMBER", length = 6)
    private String streetNumber;

    @Column(name = "STREET_DIR", length = 2)
    private String streetDirection;

    @Column(name = "STREET_NAME", length = 20)
    private String streetName;

    @Column(name = "STREET_SUFFIX", length = 4)
    private String streetSuffix;

    @Column(name = "CITY_NAME", length = 20)
    private String cityName;

    @Column(name = "STATE_CODE", length = 2)
    private String stateCode;

    @Column(name = "ZIP_CODE", length = 9)
    private String zipCode;

    @Column(name = "EVENT_DATE")
    private LocalDate eventDate;
}