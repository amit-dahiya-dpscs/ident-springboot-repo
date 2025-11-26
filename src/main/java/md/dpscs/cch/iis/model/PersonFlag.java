package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "person_flag")
@Data
public class PersonFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long flagId;

    private Long personId; // FK

    private String flagName; // e.g., 'FLAG_1'

    private String flagValue; // 'Y' or 'N'

    private LocalDate lastUpdateDate;
}
