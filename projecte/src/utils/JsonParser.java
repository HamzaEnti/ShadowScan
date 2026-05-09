package utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mini-parser JSON sense dependències externes.
 *
 * Suporta el subset de JSON necessari per a les integracions de v1.2 (NVD CVE
 * API) i v2.0 (perfils, REST API, missatges entre coordinator/worker).
 * Retorna estructures Java natives:
 *   - Object  → Map&lt;String, Object&gt;  (LinkedHashMap, ordre preservat)
 *   - Array   → List&lt;Object&gt;
 *   - String  → String
 *   - Number  → Double o Long
 *   - true/false/null → Boolean / null
 *
 * No és apte per a JSON5, comentaris ni números amb precisió arbitrària.
 */
public final class JsonParser {

    public static Object parse(String src) {
        Parser p = new Parser(src);
        p.skipWs();
        Object v = p.readValue();
        p.skipWs();
        if (p.pos < p.src.length()) {
            throw new IllegalArgumentException("JSON: contingut inesperat al final (pos=" + p.pos + ")");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String src) {
        Object v = parse(src);
        if (!(v instanceof Map)) throw new IllegalArgumentException("JSON: s'esperava un objecte");
        return (Map<String, Object>) v;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String src) {
        Object v = parse(src);
        if (!(v instanceof List)) throw new IllegalArgumentException("JSON: s'esperava un array");
        return (List<Object>) v;
    }

    /**
     * Path-style accessor: get(map, "a.b.c") → ((map[a])[b])[c], retornant
     * null si qualsevol nivell falla. Conveniència per a respostes NVD.
     */
    @SuppressWarnings("unchecked")
    public static Object get(Object root, String path) {
        if (root == null || path == null) return null;
        Object cur = root;
        for (String key : path.split("\\.")) {
            if (cur instanceof Map) cur = ((Map<String, Object>) cur).get(key);
            else if (cur instanceof List) {
                try { cur = ((List<Object>) cur).get(Integer.parseInt(key)); }
                catch (Exception e) { return null; }
            } else return null;
            if (cur == null) return null;
        }
        return cur;
    }

    public static String getString(Object root, String path) {
        Object v = get(root, path);
        return (v == null) ? null : v.toString();
    }

    public static Double getDouble(Object root, String path) {
        Object v = get(root, path);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) try { return Double.parseDouble((String) v); } catch (Exception e) { return null; }
        return null;
    }

    private JsonParser() {}

    private static final class Parser {
        final String src;
        int pos;

        Parser(String s) { this.src = s; this.pos = 0; }

        void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }

        Object readValue() {
            skipWs();
            if (pos >= src.length()) throw err("EOF inesperat");
            char c = src.charAt(pos);
            switch (c) {
                case '{': return readObject();
                case '[': return readArray();
                case '"': return readString();
                case 't': case 'f': return readBool();
                case 'n': return readNull();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) return readNumber();
                    throw err("Caràcter inesperat: " + c);
            }
        }

        Map<String, Object> readObject() {
            expect('{');
            Map<String, Object> m = new LinkedHashMap<>();
            skipWs();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                String k = readString();
                skipWs();
                expect(':');
                Object v = readValue();
                m.put(k, v);
                skipWs();
                char c = src.charAt(pos++);
                if (c == ',') continue;
                if (c == '}') return m;
                throw err("S'esperava , o }");
            }
        }

        List<Object> readArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWs();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                list.add(readValue());
                skipWs();
                char c = src.charAt(pos++);
                if (c == ',') continue;
                if (c == ']') return list;
                throw err("S'esperava , o ]");
            }
        }

        String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= src.length()) throw err("Escape incomplet");
                    char e = src.charAt(pos++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            if (pos + 4 > src.length()) throw err("Unicode escape incomplet");
                            int cp = Integer.parseInt(src.substring(pos, pos + 4), 16);
                            sb.append((char) cp);
                            pos += 4;
                            break;
                        default: throw err("Escape desconegut: \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw err("String sense tancar");
        }

        Object readNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < src.length() && "0123456789.eE+-".indexOf(src.charAt(pos)) >= 0) pos++;
            String s = src.substring(start, pos);
            if (s.contains(".") || s.contains("e") || s.contains("E")) return Double.parseDouble(s);
            try { return Long.parseLong(s); }
            catch (NumberFormatException e) { return Double.parseDouble(s); }
        }

        Boolean readBool() {
            if (src.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            throw err("Boolean invàlid");
        }

        Object readNull() {
            if (src.startsWith("null", pos)) { pos += 4; return null; }
            throw err("S'esperava null");
        }

        char peek() { return pos < src.length() ? src.charAt(pos) : '\0'; }

        void expect(char c) {
            if (pos >= src.length() || src.charAt(pos) != c) throw err("S'esperava " + c);
            pos++;
        }

        IllegalArgumentException err(String msg) {
            return new IllegalArgumentException("JSON@" + pos + ": " + msg);
        }
    }
}
