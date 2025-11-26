package md.dpscs.cch.iis.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SearchCriteriaDTO implements Serializable {
    private String typeOfRequest;
    private String fullName;
    private String dob;
    private String sid;
    private String fbiNumber;
    private String ssn;
    private String raceCode;
    private String sexCode;
}
