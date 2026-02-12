package md.dpscs.cch.iis.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class ExpungementRequest {
    @NotBlank(message = "Delete Type is required (PARTIAL, ENTIRE, DOWNGRADE)")
    private String deleteType;

    // Common Fields
    private Long systemId;
    private Long documentId; // For Part Cancel
    @NotBlank(message = "Reason for deletion is required")
    private String reason;

    // Downgrade / Part Cancel Specifics
    private String cogentPcn;
    private String cogentPcn2;
    private String courtCaseNumber;
    private String charge;

    // Audit Info
    private String username;
    private String userIp;

    private String identification;
    private String ucn;
    private String comments;

    private String requestingUnit;
}