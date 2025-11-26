package md.dpscs.cch.iis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class DocumentDTO implements Serializable {
    private String documentType;
    private String documentNumber;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate documentDate;
}
