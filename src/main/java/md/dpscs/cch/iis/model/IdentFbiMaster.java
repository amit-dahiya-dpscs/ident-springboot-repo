package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Maps to Legacy Table: AD.ADT_FBIMT
 * Purpose: Determines FBI Ownership. If a SID exists here, Expungement/Downgrade
 * logic branches to write to FBDGR instead of sending a DRS message.
 */
@Entity
@Data
@Table(name = "T_IDENT_FBI_MASTER")
public class IdentFbiMaster {

    @Id
    @Column(name = "FT_SID", length = 10, nullable = false)
    private String sid;

    @Column(name = "FBI_NUMBER", length = 9)
    private String fbiNumber;

    @Column(name = "DATE_ADDED")
    private String dateAdded;
}