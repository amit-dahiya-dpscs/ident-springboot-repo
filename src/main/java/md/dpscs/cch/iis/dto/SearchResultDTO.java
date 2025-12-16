package md.dpscs.cch.iis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SearchResultDTO {
    private Long systemId;
    private String sidNumber;
    private String fbiNumber;
    private String formattedName; // "Last, First Middle"
    private String race;
    private String sex;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate dateOfBirth;
    private String printType; // Display value for Fingerprint (e.g. "\W\W\")
    private boolean isAliasMatch; // True if the search matched an alias, not the primary name
}