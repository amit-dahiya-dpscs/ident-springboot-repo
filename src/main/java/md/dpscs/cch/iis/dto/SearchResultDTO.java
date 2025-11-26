package md.dpscs.cch.iis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class SearchResultDTO implements Serializable {
    private Long personId;
    private String sidNumber;
    private String name;
    private String sex;
    private String race;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate dateOfBirth;

    private String printType;
    private String primaryHenry;
}
