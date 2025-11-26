package md.dpscs.cch.iis.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class FlagDTO implements Serializable {
    private String flagName;
    private String flagValue;
    private LocalDate lastUpdateDate;
}
