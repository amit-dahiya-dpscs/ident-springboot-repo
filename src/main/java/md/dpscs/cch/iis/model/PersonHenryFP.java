package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "person_henry_fp")
@Data
public class PersonHenryFP {
    @Id
    private Long personId;

    private String primaryHenry;

    private String henryClassification;
}
