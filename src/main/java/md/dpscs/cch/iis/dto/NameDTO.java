package md.dpscs.cch.iis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class NameDTO {
    private Long id;
    private String nameType; // 'P' = Primary, 'A' = Alias
    private String lastName;
    private String firstName;
    private String middleName;
    private String middleInitial;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate dateOfBirth;
    private String race;
    private String sex;
}