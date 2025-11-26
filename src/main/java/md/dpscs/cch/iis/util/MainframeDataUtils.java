package md.dpscs.cch.iis.util;

import org.apache.commons.codec.language.Soundex;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class MainframeDataUtils {

    // Fingerprint mapping derived from II0400C (e.g., 1->A, 3->\)
    private static final String FP_MAPPING = "1A2W3\\4/5X8?9U0U";

    private final Soundex standardSoundex = new Soundex();

    /**
     * Executes the industry-standard Soundex calculation (Apache Commons Codec).
     */
    public String calculateStandardSoundex(String name) {
        if (name == null || name.isEmpty()) return null;
        return standardSoundex.encode(name);
    }

    /**
     * Implements the exact character-by-character replacement logic from II0400C
     * for MAFIS fingerprint codes for display.
     */
    public String convertMafisHandToDisplay(String numericHand) {
        if (numericHand == null || numericHand.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : numericHand.toCharArray()) {
            int index = FP_MAPPING.indexOf(c);
            if (index % 2 == 0) {
                sb.append(FP_MAPPING.charAt(index + 1));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}