package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "T_IDENT_DOCUMENTS")
@Data
public class IdentDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DocID")
    private Long docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SystemID")
    private IdentMaster master;

    @Column(name = "DocCategory")
    private String docCategory; // 'ARREST', 'INDEX', 'REFER'

    @Column(name = "DocumentType")
    private String documentType;

    @Column(name = "DocumentNumber")
    private String documentNumber;

    @Column(name = "DocumentDate")
    private LocalDate documentDate;

    @Column(name = "Description")
    private String description;
}