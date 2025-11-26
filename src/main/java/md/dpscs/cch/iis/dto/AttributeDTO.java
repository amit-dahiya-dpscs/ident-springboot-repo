package md.dpscs.cch.iis.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class AttributeDTO implements Serializable {
    private String attributeType; // CAUTION or SCAR_MARK_TATTOO
    private String code;
    private String description;
}
