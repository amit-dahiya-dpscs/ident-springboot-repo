package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "person")
@Data
public class Person {
    @Id
    private Long personId; // ID_SYS_NUM

    private String stateId; // ID_SID

    private String fbiNumber; // ID_FBI_NUM

    private String recordType; // ID_RECORD_TYPE

    private LocalDate dateOfBirth; // Core DOB (for search index)

    private String raceCode; // ID_RACE_CD

    private String sexCode; // ID_SEX_CD

    private String height; // ID_HEIGHT

    private String weight; // ID_WEIGHT

    private String eyeColorCode; // ID_EYE_COLOR_CD

    private String hairColorCode; // ID_HAIR_COLOR_CD

    private String skinToneCode; // ID_SKIN_COMPLEX_CD

    private String placeOfBirthCode; // ID_POB_CD

    private String citizenshipCode; // ID_COC_CD

    private String streetNumber; // ID_STREET_NUM

    private String cityName; // ID_CITY_NM

    private String zipCode; // ID_ZIP

    private String comments; // ID_PERSON_COMMENTS

    private String disseminationFlag; // ID_DISS_FLAG

    private String mugshotFlag; // ID_MUGSHOT_FLAG

    private String dnaFlag; // ID_DNA_FLAG

    private LocalDate lastUpdateDate; // ID_LAST_UPDATE_DT

    private LocalDateTime createdAt; // CREATED_AT
}