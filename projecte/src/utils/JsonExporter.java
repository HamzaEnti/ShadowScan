package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import model.ResultatHost;

/**
 * Exportador JSON per a ShadowScan.
 *
 * Genera dos formats:
 *   - saveToJSON()         → array simple de hosts (format original)
 *   - saveToTopology()     → format topology.json per a RedTrace
 *
 * El format topology.json inclou nodes, arestes calculades automàticament
 * i pesos basats en els ports oberts de cada host.
 *
 * Integració RedTrace — estructura generada:
 * {
 *   "metadata": { "generated_by": "ShadowScan", "timestamp": "..." },
 *   "nodes": [
 *     { "id": "192.168.1.1", "type": "host", "ports": [22,80], "risk": 0.6 }
 *   ],
 *   "edges": [
 *     { "from": "192.168.1.1", "to": "192.168.1.10", "weight": 0.4 }
 *   ],
 *   "entry_point": "192.168.1.1",
 *   "target": null
 * }
 *
 * Assisted by Claude (Anthropic) — topology format design for RedTrace integration.
 */
public class JsonExporter {

    // ─── Mapa de serveis per port ────────────────────────────────────────────
    // Permet identificar el servei associat a cada port obert
    // i calcular el pes d'explotació corresponent per a RedTrace.
    private static final int[] HIGH_RISK_PORTS   = {21, 23, 445, 3389, 5900};  // FTP, Telnet, SMB, RDP, VNC
    private static final int[] MEDIUM_RISK_PORTS = {22, 3306, 5432, 1433, 6379}; // SSH, MySQL, Postgres, MSSQL, Redis
    private static final int[] LOW_RISK_PORTS    = {80, 443, 8080, 8443};       // HTTP/HTTPS

    // ─── Format original (array simple) ─────────────────────────────────────

    /**
     * Exporta la llista de hosts al format JSON original de ShadowScan.
     * Compatible amb versions anteriors.
     */
    public static boolean saveToJSON(List<ResultatHost> resultats, String path) {
        if (resultats == null || resultats.isEmpty()) {
            System.out.println(">>> [EXPORT] No hi ha resultats per exportar");
            return false;
        }

        try (FileWriter writer = new FileWriter(path)) {
            writer.write("[\n");

            for (int i = 0; i < resultats.size(); i++) {
                ResultatHost h = resultats.get(i);
                writer.write("  " + h.toJson());
                if (i < resultats.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("]");
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
            return false;
        }

        try (FileWriter writer = new FileWriter(path)) {
            writer.write("[\n");

            for (int i = 0; i < resultats.size(); i++) {
                ResultatHost h = resultats.get(i);
                writer.write("  {\n");
                writer.write("    \"ip\": \"" + escapeJson(h.getIp()) + "\",\n");
                writer.write("    \"esViu\": " + h.isEsViu() + ",\n");
                writer.write("    \"estat\": \"" + escapeJson(h.getEstat().toString()) + "\",\n");
                writer.write("    \"ports\": " + toJsonArray(h.getPortsOberts()) + "\n");
                writer.write("  }");

                if (i < resultats.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("]\n");
            System.out.println(">>> [EXPORT] JSON pretty guardat a: " + path);
            return true;

        } catch (IOException e) {
            // Fix: ara sí logeja l'error en comptes de silenciar-lo
            System.err.println(">>> [ERROR] No s'ha pogut guardar el JSON pretty: " + e.getMessage());
            return false;
        }
    }

    // ─── Format RedTrace (topology.json) ────────────────────────────────────

    /**
     * Exporta els resultats al format topology.json que espera RedTrace.
     *
     * Genera automàticament:
     *   - nodes: un per cada host actiu, amb tipus inferit i risc calculat
     *   - edges: connexions entre tots els hosts de la mateixa subxarxa
     *   - weight: pes d'explotació basat en els ports oberts (0.1 = fàcil, 1.0 = difícil)
     *   - entry_point: primer host trobat (es pot canviar manualment)
     *
     * @param resultats  llista de hosts escanejats per ShadowScan
     * @param path       ruta on guardar el fitxer topology.json
     * @param entryPoint IP del node d'entrada (null = primer host actiu trobat)
     * @param target     IP del node objectiu (null = últim host actiu trobat)
     * @return true si s'ha guardat correctament
     */
    public static boolean saveToTopology(List<ResultatHost> resultats, String path,
                                          String entryPoint, String target) {
        // Filtrem només els hosts actius
        List<ResultatHost> hostsActius = new java.util.ArrayList<>();
        for (ResultatHost h : resultats) {
            if (h.isEsViu()) {
                hostsActius.add(h);
            }
        }

        if (hostsActius.isEmpty()) {
            System.out.println(">>> [EXPORT] No hi ha hosts actius per exportar com a topologia");
            return false;
        }

        // Si no s'especifiquen, usem el primer i l'últim host actiu
        String entry = (entryPoint != null) ? entryPoint : hostsActius.get(0).getIp();
        String tgt   = (target != null)     ? target     : hostsActius.get(hostsActius.size() - 1).getIp();

        try (FileWriter w = new FileWriter(path)) {
            w.write("{\n");

            // Metadata
            w.write("  \"metadata\": {\n");
            w.write("    \"generated_by\": \"ShadowScan\",\n");
            w.write("    \"version\": \"1.0\",\n");
            w.write("    \"timestamp\": \"" + LocalDateTime.now() + "\",\n");
            w.write("    \"hosts_scanned\": " + resultats.size() + ",\n");
            w.write("    \"hosts_active\": " + hostsActius.size() + "\n");
            w.write("  },\n");

            // Nodes
            w.write("  \"nodes\": [\n");
            for (int i = 0; i < hostsActius.size(); i++) {
                ResultatHost h = hostsActius.get(i);
                List<Integer> ports = h.getPortsOberts();
                double risk = calcularRisc(ports);
                String tipus = inferirTipus(ports);

                w.write("    {\n");
                w.write("      \"id\": \"" + escapeJson(h.getIp()) + "\",\n");
                w.write("      \"type\": \"" + tipus + "\",\n");
                w.write("      \"ports\": " + toJsonArray(ports) + ",\n");
                w.write("      \"services\": " + toServiceMap(ports) + ",\n");
                w.write("      \"risk\": " + String.format("%.2f", risk) + "\n");
                w.write("    }");
                if (i < hostsActius.size() - 1) w.write(",");
                w.write("\n");
            }
            w.write("  ],\n");

            // Edges — connectem tots els hosts de la mateixa subxarxa
            w.write("  \"edges\": [\n");
            boolean firstEdge = true;
            for (int i = 0; i < hostsActius.size(); i++) {
                for (int j = i + 1; j < hostsActius.size(); j++) {
                    ResultatHost src = hostsActius.get(i);
                    ResultatHost dst = hostsActius.get(j);

                    // El pes és la dificultat d'explotar la connexió:
                    // baix = fàcil (ports de risc alt oberts), alt = difícil
                    double weight = calcularPesAresta(src.getPortsOberts(), dst.getPortsOberts());

                    if (!firstEdge) w.write(",\n");
                    w.write("    {\n");
                    w.write("      \"from\": \"" + escapeJson(src.getIp()) + "\",\n");
                    w.write("      \"to\": \"" + escapeJson(dst.getIp()) + "\",\n");
                    w.write("      \"weight\": " + String.format("%.2f", weight) + "\n");
                    w.write("    }");
                    firstEdge = false;
                }
            }
            w.write("\n  ],\n");

            // Entry point i target
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

    // ─── Helpers privats ─────────────────────────────────────────────────────

    /**
     * Calcula el nivell de risc d'un host basant-se en els ports oberts.
     * Retorna un valor entre 0.1 (risc baix) i 1.0 (risc crític).
     * RedTrace utilitza aquest valor per prioritzar nodes en la ruta d'atac.
     */
    private static double calcularRisc(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) return 0.1;

        double risc = 0.1;
        for (int port : ports) {
            if (contePort(HIGH_RISK_PORTS, port))   risc += 0.25;
            else if (contePort(MEDIUM_RISK_PORTS, port)) risc += 0.15;
            else if (contePort(LOW_RISK_PORTS, port))    risc += 0.05;
            else risc += 0.02; // port desconegut
        }
        return Math.min(risc, 1.0); // cap a 1.0
    }

    /**
     * Calcula el pes d'una aresta entre dos nodes.
     * Pes baix = connexió fàcil d'explotar (ports de risc alt).
     * Pes alt  = connexió difícil (pocs ports oberts o tots d'alt risc).
     */
    private static double calcularPesAresta(List<Integer> portsSrc, List<Integer> portsDst) {
        // El pes és la inversa del risc combinat dels dos extrems
        double riscSrc = calcularRisc(portsSrc);
        double riscDst = calcularRisc(portsDst);
        double riscMig = (riscSrc + riscDst) / 2.0;

        // Invertim: risc alt → pes baix (fàcil explotar)
        double pes = 1.0 - riscMig + 0.1;
        return Math.max(0.1, Math.min(pes, 1.0));
    }

    /**
     * Infereix el tipus de node a partir dels ports oberts.
     * Ajuda a RedTrace a visualitzar millor la topologia.
     */
    private static String inferirTipus(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) return "host";
        boolean teHttp   = ports.contains(80) || ports.contains(443) || ports.contains(8080);
        boolean teDb     = ports.contains(3306) || ports.contains(5432) || ports.contains(1433);
        boolean teSsh    = ports.contains(22);
        boolean teSmb    = ports.contains(445) || ports.contains(139);
        boolean teRdp    = ports.contains(3389);
        boolean teDns    = ports.contains(53);

        if (teDns)   return "router";
        if (teSmb)   return "fileserver";
        if (teDb)    return "database";
        if (teHttp && !teSsh) return "webserver";
        if (teRdp)   return "workstation";
        return "host";
    }

    /**
     * Genera un objecte JSON amb el mapatge port → servei.
     * Exemple: {"22":"ssh","80":"http","443":"https"}
     */
    private static String toServiceMap(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int port : ports) {
            if (!first) sb.append(", ");
            sb.append("\"").append(port).append("\": \"").append(nomServei(port)).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String nomServei(int port) {
        switch (port) {
            case 21:   return "ftp";
            case 22:   return "ssh";
            case 23:   return "telnet";
            case 25:   return "smtp";
            case 53:   return "dns";
            case 80:   return "http";
            case 110:  return "pop3";
            case 139:  return "netbios";
            case 143:  return "imap";
            case 443:  return "https";
            case 445:  return "smb";
            case 1433: return "mssql";
            case 3306: return "mysql";
            case 3389: return "rdp";
            case 5432: return "postgres";
            case 5900: return "vnc";
            case 6379: return "redis";
            case 8080: return "http-alt";
            case 8443: return "https-alt";
            default:   return "unknown-" + port;
        }
    }

    /**
     * Converteix una llista d'enters en un array JSON: [22, 80, 443]
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
     * Escapa caràcters especials per JSON vàlid.
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static boolean contePort(int[] array, int port) {
        for (int p : array) {
            if (p == port) return true;
        }
        return false;
    }
}
