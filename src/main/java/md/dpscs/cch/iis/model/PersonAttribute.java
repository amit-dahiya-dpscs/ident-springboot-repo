package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "person_attribute")
@Data
public class PersonAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attributeId;

    private Long personId;

    private String attributeType; // CAUTION or SCAR_MARK_TATTOO

    private String code;

    private String description;
}
