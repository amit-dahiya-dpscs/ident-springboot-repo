package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "document_general_ref")
@Data
public class DocumentGeneralRef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    private Long personId;

    private String documentType;

    private String documentNumber;

    private LocalDate documentDate;

    private String description;
}
