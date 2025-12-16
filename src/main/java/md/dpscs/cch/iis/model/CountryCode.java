package md.dpscs.cch.iis.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "T_REF_COUNTRY") // Clean, standard naming
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryCode {

    @Id
    @Column(name = "CountryCode", length = 3) // Standard ISO 3-char code
    private String code;

    @Column(name = "Description", nullable = false)
    private String description;
}