package md.dpscs.cch.iis.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SecondaryIDDTO implements Serializable {
    private String idType; // SSN, DriverLicense, Misc
    private String idValue;
    private String issuingSource;
}
