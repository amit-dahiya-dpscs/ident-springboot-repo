package md.dpscs.cch.iis.dto;

import lombok.Data;

@Data
public class FingerprintDTO {
    private String type; // "HENRY" or "NCIC"
    private String primary; // e.g., "18 W 1 U" (Only for Henry)
    private String classification; // The full pattern code
}