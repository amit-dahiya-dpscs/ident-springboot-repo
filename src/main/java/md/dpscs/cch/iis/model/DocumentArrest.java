package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "document_arrest")
@Data
public class DocumentArrest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    private Long personId; // FK

    private String documentType; // AR_DOC_TYPE

    private String documentNumber; // AR_DOC_NUM

    private LocalDate documentDate; // AR_DOC_DT
    
    private String description; // AR_DOC_DESC
}
