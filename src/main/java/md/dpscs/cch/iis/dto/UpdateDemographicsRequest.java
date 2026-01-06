package md.dpscs.cch.iis.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDemographicsRequest {
    // --- Personal Identifiers (II1100C) ---
    @Size(max = 1, message = "Race must be 1 character")
    private String race;

    @Size(max = 1, message = "Sex must be 1 character")
    private String sex;

    @Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$", message = "DOB must be MM/DD/YYYY")
    private String dob;

    @Size(max = 2, message = "POB must be 2 characters")
    private String placeOfBirth;

    @Pattern(regexp = "^\\d{3}$", message = "Height must be 3 digits (e.g., 509)")
    private String height;

    @Pattern(regexp = "^\\d{3}$", message = "Weight must be 3 digits (e.g., 160)")
    private String weight;

    @Size(max = 3)
    private String eyeColor;

    @Size(max = 3)
    private String hairColor;

    @Size(max = 3)
    private String skinTone;

    @Size(max = 2)
    private String citizenship;

    private String comments;

    @Pattern(regexp = "^$|^[A-Za-z0-9]+$", message = "Invalid FBI Number format")
    private String fbiNumber;

    // --- Address Fields (II1100C / DetailService) ---
    @Size(max = 5)
    private String streetNumber;

    @Size(max = 2)
    private String streetDirection;

    @Size(max = 25) // Adjusted for standard street name length
    private String streetName;

    @Size(max = 2)
    private String streetSuffix;

    @Size(max = 20)
    private String city;

    @Size(max = 2)
    private String state;

    @Size(max = 5)
    private String zip;
}