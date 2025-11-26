package md.dpscs.cch.iis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class NameDTO implements Serializable {
    private String lastName;
    private String firstName;
    private String middleInit;
    private String restMiddleName;
    private String soundexCode;
    private String fingerprintMafis;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate birthDate;
}
