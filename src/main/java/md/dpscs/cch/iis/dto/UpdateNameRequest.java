package md.dpscs.cch.iis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateNameRequest {
    @NotBlank(message = "Last Name is required")
    private String lastName;

    @NotBlank(message = "First Name is required")
    private String firstName;

    private String middleName;

    // Used for the UCN trigger logic in II1100C
    private String ucn;
}