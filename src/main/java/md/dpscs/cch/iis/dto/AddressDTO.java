package md.dpscs.cch.iis.dto;

import lombok.Data;

@Data
public class AddressDTO {
    private String fullAddress; // Assembled string for display
    private String streetNumber;
    private String streetDirection;
    private String streetName;
    private String streetSuffix;
    private String city;
    private String state;
    private String zip;
    private Boolean isCurrent;
}