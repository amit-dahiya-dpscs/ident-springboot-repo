package md.dpscs.cch.iis.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "T_REF_RACE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RaceCode {

    @Id
    @Column(name = "RaceCode", length = 1)
    private String code;

    @Column(name = "Description", nullable = false)
    private String description;
}