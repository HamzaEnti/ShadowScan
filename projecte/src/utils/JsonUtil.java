package utils;

import java.util.Collection;
import java.util.Locale;

/**
 * Utilitats compartides per generar JSON segur.
 *
 * Centralitzem aquí l'escape per evitar duplicar-lo a ResultatHost,
 * JsonExporter i futurs serialitzadors. Ús d'un format invariant
 * (Locale.ROOT) per als floats: així el risc sempre s'imprimeix com
 * "0.5" i mai com "0,5", independentment de la configuració del sistema.
 */
public final class JsonUtil {

    public static final Locale JSON_LOCALE = Locale.ROOT;

    private JsonUtil() {}

    /**
     * Escapa caràcters especials per JSON estricte (RFC 8259).
     */
    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format(JSON_LOCALE, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Format invariant amb 2 decimals: garanteix punt decimal (no coma).
     */
    public static String formatDouble(double v) {
        return String.format(JSON_LOCALE, "%.2f", v);
    }

    /**
     * Serialitza una col·lecció d'enters com a array JSON: [22, 80, 443]
     */
    public static String intArray(Collection<Integer> ints) {
        if (ints == null || ints.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Integer n : ints) {
            if (!first) sb.append(", ");
            sb.append(n);
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
