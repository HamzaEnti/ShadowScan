package utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import model.ResultatHost;

/**
 * Exportador JSON per a ShadowScan.
 *
 * Genera dos formats:
 *   - saveToJSON() / saveToJSONPretty() → array simple de hosts (compatible)
 *   - saveToTopology()                  → format topology.json per a RedTrace
 *
 * Versió blindada: locale invariant per als floats, UTF-8 explícit, escape
 * complet de control chars, validació de paths/IPs, lookup O(1) de ports,
 * creació automàtica de directoris i timestamps UTC.
 *
 */
public class JsonExporter {

    /* ─── Configuració ──────────────────────────────────────────────────── */

    /**
     * Locale invariant per a tots els floats.
     * IMPORTANT: NO canviar a Locale.getDefault(). Si el sistema està en
     * català/castellà, "%.2f" emetria "0,12" en lloc de "0.12" i el JSON
     * resultant seria invàlid (json.loads de Python falla amb JSONDecodeError).
     */
    private static final Locale JSON_LOCALE = Locale.ROOT;

    /* ─── Categories de ports per al càlcul de risc ─────────────────────── */

    private static final Set<Integer> HIGH_RISK_PORTS = Set.of(
        21,    // FTP (clear-text)
        23,    // Telnet (clear-text)
        135,   // RPC endpoint mapper
        137,   // NetBIOS Name Service
        138,   // NetBIOS Datagram Service
        139,   // NetBIOS Session Service
        445,   // SMB
        3389,  // RDP
        5900   // VNC
    );

    private static final Set<Integer> MEDIUM_RISK_PORTS = Set.of(
        22,    // SSH
        25,    // SMTP
        110,   // POP3
        143,   // IMAP
        1433,  // MSSQL
        3306,  // MySQL
        5432,  // PostgreSQL
        6379,  // Redis
        27017  // MongoDB
    );

    private static final Set<Integer> LOW_RISK_PORTS = Set.of(
        53,    // DNS
        80,    // HTTP
        443,   // HTTPS
        8080,  // HTTP-alt
        8443   // HTTPS-alt
    );

    /* ─── Mapatge port → nom de servei ──────────────────────────────────── */

    private static final Map<Integer, String> SERVICE_NAMES = Map.ofEntries(
        Map.entry(21, "ftp"),
        Map.entry(22, "ssh"),
        Map.entry(23, "telnet"),
        Map.entry(25, "smtp"),
        Map.entry(53, "dns"),
        Map.entry(80, "http"),
        Map.entry(110, "pop3"),
        Map.entry(135, "rpc"),
        Map.entry(137, "netbios-ns"),
        Map.entry(138, "netbios-dgm"),
        Map.entry(139, "netbios-ssn"),
        Map.entry(143, "imap"),
        Map.entry(443, "https"),
        Map.entry(445, "smb"),
        Map.entry(1433, "mssql"),
        Map.entry(3306, "mysql"),
        Map.entry(3389, "rdp"),
        Map.entry(5432, "postgres"),
        Map.entry(5900, "vnc"),
        Map.entry(6379, "redis"),
        Map.entry(8080, "http-alt"),
        Map.entry(8443, "https-alt"),
        Map.entry(27017, "mongodb")
    );

    /* ─── Format original (array simple) ────────────────────────────────── */

    /**
     * Exporta la llista de hosts al format JSON original de ShadowScan.
     */
    public static boolean saveToJSON(List<ResultatHost> resultats, String path) {
        if (resultats == null || resultats.isEmpty()) {
            System.out.println(">>> [EXPORT] No hi ha resultats per exportar");
            return false;
        }
        if (path == null || path.isBlank()) {
            System.err.println(">>> [ERROR] Ruta de sortida buida");
            return false;
        }

        try (Writer w = openWriter(path)) {
            w.write("[\n");
            for (int i = 0; i < resultats.size(); i++) {
                ResultatHost h = resultats.get(i);
                w.write("  ");
                w.write(h.toJson());
                if (i < resultats.size() - 1) w.write(",");
                w.write("\n");
            }
            w.write("]\n");
            System.out.println(">>> [EXPORT] JSON guardat a: " + path);
            return true;
        } catch (IOException e) {
            System.err.println(">>> [ERROR] No s'ha pogut guardar el JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Versió pretty del format original amb indentació completa.
     */
    public static boolean saveToJSONPretty(List<ResultatHost> resultats, String path) {
        if (resultats == null || resultats.isEmpty()) {
            System.out.println(">>> [EXPORT] No hi ha resultats per exportar");
            return false;
        }
        if (path == null || path.isBlank()) {
            System.err.println(">>> [ERROR] Ruta de sortida buida");
            return false;
        }

        // Bug fix: abans aquest mètode escrivia un esquema antic (ip/esViu/
        // estat/ports) mentre que saveToJSON ja emetia el nou format amb
        // hostname i risc. Resultat: dos exports incompatibles. Ara usem
        // h.toJson() i només afegim indentació per llegibilitat.
        try (Writer w = openWriter(path)) {
            w.write("[\n");
            for (int i = 0; i < resultats.size(); i++) {
                w.write("  ");
                w.write(resultats.get(i).toJson());
                if (i < resultats.size() - 1) w.write(",");
                w.write("\n");
            }
            w.write("]\n");
            System.out.println(">>> [EXPORT] JSON pretty guardat a: " + path);
            return true;
        } catch (IOException e) {
            System.err.println(">>> [ERROR] No s'ha pogut guardar el JSON pretty: " + e.getMessage());
            return false;
        }
    }

    /* ─── Format RedTrace (topology.json) ───────────────────────────────── */

    /**
     * Exporta els resultats al format topology.json que espera RedTrace.
     *
     * @param resultats  llista de hosts escanejats per ShadowScan
     * @param path       ruta on guardar el fitxer (es crearan els directoris pares)
     * @param entryPoint IP del node d'entrada (null = primer host actiu)
     * @param target     IP del node objectiu (null = últim host actiu)
     * @return true si s'ha guardat correctament
     */
    public static boolean saveToTopology(List<ResultatHost> resultats, String path,
                                          String entryPoint, String target) {
        if (resultats == null) {
            System.err.println(">>> [ERROR] Llista de resultats nul·la");
            return false;
        }
        if (path == null || path.isBlank()) {
            System.err.println(">>> [ERROR] Ruta de sortida buida");
            return false;
        }

        // Filtrem hosts actius amb IP vàlida
        List<ResultatHost> hostsActius = new ArrayList<>();
        for (ResultatHost h : resultats) {
            if (h != null && h.isEsViu()
                && h.getIp() != null && !h.getIp().isBlank()) {
                hostsActius.add(h);
            }
        }

        if (hostsActius.isEmpty()) {
            System.out.println(">>> [EXPORT] No hi ha hosts actius per exportar com a topologia");
            return false;
        }

        // Resolució amb fallbacks
        String entry = (entryPoint != null && !entryPoint.isBlank())
            ? entryPoint
            : hostsActius.get(0).getIp();
        String tgt = (target != null && !target.isBlank())
            ? target
            : hostsActius.get(hostsActius.size() - 1).getIp();

        // Validació: entry i target han de ser hosts actius
        Set<String> ipsActives = new HashSet<>();
        for (ResultatHost h : hostsActius) ipsActives.add(h.getIp());
        if (!ipsActives.contains(entry)) {
            System.err.println(">>> [ERROR] entry_point '" + entry +
                "' no és cap host actiu de la llista");
            return false;
        }
        if (tgt != null && !ipsActives.contains(tgt)) {
            System.err.println(">>> [ERROR] target '" + tgt +
                "' no és cap host actiu de la llista");
            return false;
        }

        try (Writer w = openWriter(path)) {
            w.write("{\n");

            // ── metadata ──
            w.write("  \"metadata\": {\n");
            w.write("    \"generated_by\": \"ShadowScan\",\n");
            w.write("    \"version\": \"1.0\",\n");
            w.write("    \"timestamp\": \"" + Instant.now() + "\",\n");
            w.write("    \"hosts_scanned\": " + resultats.size() + ",\n");
            w.write("    \"hosts_active\": " + hostsActius.size() + "\n");
            w.write("  },\n");

            // ── nodes ──
            w.write("  \"nodes\": [\n");
            for (int i = 0; i < hostsActius.size(); i++) {
                ResultatHost h = hostsActius.get(i);
                List<Integer> ports = h.getPortsOberts();
                double risk = calcularRisc(ports);
                String tipus = inferirTipus(ports);

                w.write("    {\n");
                w.write("      \"id\": \"" + escapeJson(h.getIp()) + "\",\n");
                w.write("      \"type\": \"" + escapeJson(tipus) + "\",\n");
                w.write("      \"ports\": " + toJsonArray(ports) + ",\n");
                w.write("      \"services\": " + toServiceMap(ports) + ",\n");
                w.write("      \"risk\": " + fmtDouble(risk) + "\n");
                w.write("    }");
                if (i < hostsActius.size() - 1) w.write(",");
                w.write("\n");
            }
            w.write("  ],\n");

            // ── edges (graf complet entre hosts actius) ──
            w.write("  \"edges\": [\n");
            boolean firstEdge = true;
            for (int i = 0; i < hostsActius.size(); i++) {
                for (int j = i + 1; j < hostsActius.size(); j++) {
                    ResultatHost src = hostsActius.get(i);
                    ResultatHost dst = hostsActius.get(j);
                    double weight = calcularPesAresta(
                        src.getPortsOberts(), dst.getPortsOberts());

                    if (!firstEdge) w.write(",\n");
                    w.write("    {\n");
                    w.write("      \"from\": \"" + escapeJson(src.getIp()) + "\",\n");
                    w.write("      \"to\": \"" + escapeJson(dst.getIp()) + "\",\n");
                    w.write("      \"weight\": " + fmtDouble(weight) + "\n");
                    w.write("    }");
                    firstEdge = false;
                }
            }
            w.write("\n  ],\n");

            // ── entry / target ──
            w.write("  \"entry_point\": \"" + escapeJson(entry) + "\",\n");
            if (tgt != null) {
                w.write("  \"target\": \"" + escapeJson(tgt) + "\"\n");
            } else {
                w.write("  \"target\": null\n");
            }

            w.write("}\n");

            System.out.println(">>> [EXPORT] Topologia RedTrace guardada a: " + path);
            System.out.println(">>> [EXPORT] Nodes: " + hostsActius.size() +
                               " | Entry: " + entry + " | Target: " + tgt);
            return true;
        } catch (IOException e) {
            System.err.println(">>> [ERROR] No s'ha pogut guardar la topologia: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sobrecàrrega sense entry_point ni target — els infereix automàticament.
     */
    public static boolean saveToTopology(List<ResultatHost> resultats, String path) {
        return saveToTopology(resultats, path, null, null);
    }

    /* ─── Helpers privats ───────────────────────────────────────────────── */

    /**
     * Obre un Writer en UTF-8 amb buffering, creant directoris pares si calen.
     */
    private static Writer openWriter(String path) throws IOException {
        Path p = Paths.get(path);
        Path parent = p.getParent();
        if (parent != null) Files.createDirectories(parent);
        return new BufferedWriter(new OutputStreamWriter(
            Files.newOutputStream(p), StandardCharsets.UTF_8));
    }

    /**
     * Format de double amb 2 decimals, locale invariant (sempre punt).
     */
    private static String fmtDouble(double v) {
        return String.format(JSON_LOCALE, "%.2f", v);
    }

    /**
     * Calcula el risc d'un host (0.10 - 1.00) segons els ports oberts.
     */
    private static double calcularRisc(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) return 0.10;
        double risc = 0.10;
        for (int port : ports) {
            if (HIGH_RISK_PORTS.contains(port))        risc += 0.25;
            else if (MEDIUM_RISK_PORTS.contains(port)) risc += 0.15;
            else if (LOW_RISK_PORTS.contains(port))    risc += 0.05;
            else                                       risc += 0.02;
        }
        return Math.min(risc, 1.0);
    }

    /**
     * Pes d'una aresta: invers del risc combinat (0.10 - 1.00).
     * Pes baix → connexió fàcil d'explotar; pes alt → difícil.
     */
    private static double calcularPesAresta(List<Integer> portsSrc, List<Integer> portsDst) {
        double riscMig = (calcularRisc(portsSrc) + calcularRisc(portsDst)) / 2.0;
        double pes = 1.0 - riscMig + 0.1;
        return Math.max(0.10, Math.min(pes, 1.0));
    }

    /**
     * Infereix el tipus de node per als visualitzadors de RedTrace.
     */
    private static String inferirTipus(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) return "host";
        boolean teHttp = ports.contains(80)  || ports.contains(443)
                      || ports.contains(8080) || ports.contains(8443);
        boolean teDb   = ports.contains(3306) || ports.contains(5432)
                      || ports.contains(1433) || ports.contains(27017);
        boolean teSsh  = ports.contains(22);
        boolean teSmb  = ports.contains(445)  || ports.contains(139);
        boolean teRdp  = ports.contains(3389);
        boolean teDns  = ports.contains(53);

        if (teDns)            return "router";
        if (teSmb)            return "fileserver";
        if (teDb)             return "database";
        if (teHttp && !teSsh) return "webserver";
        if (teRdp)            return "workstation";
        return "host";
    }

    /**
     * {"22":"ssh", "80":"http", ...}
     */
    private static String toServiceMap(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int port : ports) {
            if (!first) sb.append(", ");
            sb.append("\"").append(port).append("\": \"")
              .append(escapeJson(nomServei(port))).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String nomServei(int port) {
        return SERVICE_NAMES.getOrDefault(port, "unknown-" + port);
    }

    /**
     * [22, 80, 443]
     */
    private static String toJsonArray(List<Integer> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escapa caràcters especials per JSON estricte (RFC 8259).
     * Inclou control chars (menors de 0x20) com a \\uXXXX.
     */
    private static String escapeJson(String s) {
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
}
