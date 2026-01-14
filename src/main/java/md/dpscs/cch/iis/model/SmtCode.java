package md.dpscs.cch.iis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "T_REF_SMT_CODE") // Maps to Legacy PST_SMTCD
@Data
public class SmtCode {

    @Id
    @Column(name = "SMT_CD", length = 10, nullable = false)
    private String code;

    @Column(name = "SMT_CATEGORY", length = 50)
    private String category;

    @Column(name = "SMT_BREAKDOWN", length = 50)
    private String breakdown;

    /**
     * Helper to reconstruct the full description from legacy parts.
     */
    public String getFullDescription() {
        String cat = category != null ? category.trim() : "";
        String brk = breakdown != null ? breakdown.trim() : "";

        if (cat.isEmpty() && brk.isEmpty()) return "";
        if (cat.isEmpty()) return brk;
        if (brk.isEmpty()) return cat;

        return cat + " " + brk;
    }
}