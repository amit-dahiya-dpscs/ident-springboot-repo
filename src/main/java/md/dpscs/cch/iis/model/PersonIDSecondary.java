package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "person_id_secondary")
@Data
public class PersonIDSecondary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long secondaryId;

    private Long personId;

    private String idType; // SSN, DriverLicense, Misc

    private String idValue;

    private String issuingSource;
}