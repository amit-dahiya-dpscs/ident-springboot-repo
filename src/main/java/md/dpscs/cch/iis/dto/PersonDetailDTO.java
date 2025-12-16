package md.dpscs.cch.iis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonDetailDTO {
    // --- Header Info ---
    private Long systemId;
    private String sid;
    private String fbiNumber;
    private String recordType; // 'C' = Criminal, 'J' = Juvenile
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDateTime lastUpdated;
    private String cautionFlag; // "YES" or "NO" based on flags
    private String comments;

    // --- Personal Identifiers (Demographics) ---
    private String race;
    private String sex;
    private String height;
    private String weight;
    private String eyeColor;
    private String hairColor;
    private String skinTone;
    private String placeOfBirth;
    private String citizenship;
    private String patternRight;
    private String patternLeft;

    // --- Sections (Lists) ---
    private List<NameDTO> namesAndAliases;
    private List<AddressDTO> addressHistory;
    private List<FingerprintDTO> fingerprints; // Henry & NCIC

    // --- Appended ID Section ---
    private List<FlagDTO> flags; // Cautions & Status Flags
    private List<SsnDTO> ssnHistory;
    private List<DriverLicenseDTO> driverLicenses;
    private List<AttributeDTO> scarsAndMarks;
    private List<AltDOBDTO> alternateDOBs;
    private List<SecondaryIDDTO> secondaryIdentifiers; // Misc Numbers

    // --- Reference Section ---
    private List<DocumentDTO> arrestDocuments;
    private List<DocumentDTO> indexDocuments;
    private List<DocumentDTO> generalReferences;
}