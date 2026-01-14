package md.dpscs.cch.iis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class DocumentDTO {
    private Long id;
    private String category; // "ARREST", "INDEX", "REFER"
    private String documentType;
    private String documentNumber;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate documentDate;
    private String description;
}