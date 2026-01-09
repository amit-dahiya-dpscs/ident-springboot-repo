package md.dpscs.cch.iis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateNameRequest {

    private Long id;

    private Boolean isMarkedForDeletion;

    @NotBlank(message = "Last Name is required")
    private String lastName;

    @NotBlank(message = "First Name is required")
    private String firstName;

    private String middleName;

    private String nameType; // 'A' = Alias, 'P' = Primary

    private String ucn;
}