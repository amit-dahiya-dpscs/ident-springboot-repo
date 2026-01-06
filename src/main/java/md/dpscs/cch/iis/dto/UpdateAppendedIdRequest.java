package md.dpscs.cch.iis.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateAppendedIdRequest {
    private List<String> cautions;      // Caution Codes
    private List<String> dobs;          // Alternate DOBs
    private List<AttributeDTO> scarsMarks; // SMT Codes + Desc
    private List<String> ssns;          // SSNs
    private List<SecondaryIDDTO> miscNumbers; // Misc + Drivers License
}