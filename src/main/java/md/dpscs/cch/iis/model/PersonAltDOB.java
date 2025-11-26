package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "person_alt_dob")
@Data
public class PersonAltDOB {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long altDobId;

    private Long personId;

    private LocalDate dob;
}
