package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "person_ncic_fp")
@Data
public class PersonNCICFP {
    @Id
    private Long personId;

    private String ncicPattern;
}