package md.dpscs.cch.iis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class PersonDetailDTO implements Serializable {
    // Core Person Identifiers (from Person entity)
    private Long personId;
    private String stateId;
    private String fbiNumber;
    private String recordType;
    private String comments;
    private String mugshotFlag;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate primaryDateOfBirth;

    // Nested DTOs (Representing data aggregated from 12 other tables)

    // 1. PersonName
    private List<NameDTO> namesAndAliases;

    // 2. PersonFlag
    private List<FlagDTO> flags;

    // 3. PersonAttribute (Cautions/Scars)
    private List<AttributeDTO> cautionsAndScars;

    // 4. PersonIDSecondary (SSN/DL/Misc)
    private List<SecondaryIDDTO> secondaryIdentifiers;

    // 5. PersonAltDOB
    private List<AltDOBDTO> alternateDOBs;

    // 6-8. Documents
    private List<DocumentDTO> arrestDocuments;
    private List<DocumentDTO> indexDocuments;
    private List<DocumentDTO> generalReferences;
}

