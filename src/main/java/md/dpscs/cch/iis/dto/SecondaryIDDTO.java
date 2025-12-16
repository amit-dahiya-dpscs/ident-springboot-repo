package md.dpscs.cch.iis.dto;

import lombok.Data;

@Data
public class SecondaryIDDTO {
    private String idType;  // e.g., "SID", "DOC"
    private String idValue; // The actual number
}