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

    /**
     * Converts Display Characters (A, W, X...) back to Mainframe Storage Numbers (1, 2, 5...).
     * Logic derived from II1100C Paragraph EDIT-FINGERS.
     * * Mappings:
     * A -> 1
     * W -> 2
     * \ -> 3
     * / -> 4
     * X -> 5
     * ? -> 8
     * U -> 9
     * Space -> Space (Removal)
     */
    public String convertDisplayToMafisHand(String displayHand) {
        if (displayHand == null) return null;
        if (displayHand.isBlank()) return ""; // Handle removal scenario

        StringBuilder sb = new StringBuilder();
        for (char c : displayHand.toUpperCase().toCharArray()) {
            switch (c) {
                case 'A': sb.append('1'); break;
                case 'W': sb.append('2'); break;
                case '\\': sb.append('3'); break;
                case '/': sb.append('4'); break;
                case 'X': sb.append('5'); break;
                case '?': sb.append('8'); break;
                case 'U': sb.append('9'); break; // Input U maps to 9
                case ' ': sb.append(' '); break; // Preserve spaces if partial
                default:
                    throw new IllegalArgumentException("Invalid Fingerprint Pattern Character: " + c);
            }
        }
        return sb.toString();
    }
}