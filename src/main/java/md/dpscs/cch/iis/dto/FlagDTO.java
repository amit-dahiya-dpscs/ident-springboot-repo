package md.dpscs.cch.iis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlagDTO {
    private String code; // e.g., "A", "B"
    private String type; // "CAUTION" or "STATUS"
    private String description; // e.g., "Armed"
}