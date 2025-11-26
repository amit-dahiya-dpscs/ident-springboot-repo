package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "person_name")
@Data
public class PersonName {
    @EmbeddedId
    private PersonNameId id;

    private String lastName; // NA_LAST_NM

    private String firstName; // NA_FIRST_NM

    private String middleInit; // NA_MIDDLE_INIT

    private String restMiddleName; // NA_REST_MIDDLE_NM

    private String soundexCode; // NA_SOUNDEX_CD

    private LocalDate birthDate; // NA_BIRTH_DT

    private String fingerprintMafis; // NA_MAFIS_FPRINT

    @Embeddable
    @Data
    public static class PersonNameId implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long personId; // FK
        private Long insertSeqNum; // PK component (1=Primary, >1=Alias)
    }
}