package md.dpscs.cch.iis.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "T_REF_CAUTION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CautionCode {

    @Id
    @Column(name = "CautionCode", length = 1)
    private String code; // e.g., "A"

    @Column(name = "Description", nullable = false)
    private String description; // e.g., "Armed"
}