package md.dpscs.cch.iis.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;

@Data
public class SearchCriteriaDTO implements Serializable {

    // --- 1. Request Type (Critical for logic) ---
    // Values: "SDX" or "" (Empty/Space)
    private String typeOfRequest;

    // --- 2. Name Input (From Frontend) ---
    private String fullName;

    // --- 3. Date Input ---
    // Must be String to support "MM/dd/yyyy" OR just "yyyy" (for SDX)
    private String dob;

    // --- 4. Identifiers with Validation ---

    // Allow empty string OR valid SID format
    @Pattern(regexp = "^$|^(SID-)?\\d+$", message = "Invalid SID format")
    private String sid;

    // Allow empty string OR Alphanumeric (Max 10 chars)
    @Pattern(regexp = "^$|^[A-Za-z0-9]{1,10}$", message = "Invalid FBI Number format")
    private String fbiNumber;

    // Allow empty string OR exactly 9 digits
    @Pattern(regexp = "^$|^\\d{9}$", message = "SSN must be 9 digits")
    private String ssn;

    @Size(max = 22, message = "DL Number too long")
    private String dlNumber;

    // Legacy Spec 3001: State is 3 chars (e.g., "MD ")
    @Size(max = 3, message = "DL State too long")
    private String dlState;

    // --- 5. Demographics ---
    // Renamed from 'raceCode' to 'race' to match React Payload
    @Size(max = 1, message = "Race code must be 1 character")
    private String race;

    // Renamed from 'sexCode' to 'sex' to match React Payload
    @Size(max = 1, message = "Sex code must be 1 character")
    private String sex;

    // --- 6. Internal / Parsed Fields (Optional) ---
    // These are not sent by React, but useful if you parse them in the Controller
    private String lastName;
    private String firstName;
}