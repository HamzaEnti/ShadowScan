package services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import model.CveEntry;
import utils.JsonParser;

/**
 * Consulta la base de dades CVE de NIST (NVD 2.0 API) per identificar
 * vulnerabilitats associades a un servei detectat.
 *
 * Característiques:
 *   - Cache en memòria amb TTL configurable (per defecte 1 hora) per evitar
 *     bombardejar l'API i respectar el rate-limit (5 req/30s sense API key).
 *   - Mode offline amb un mini-catàleg local de CVEs notables per quan no hi
 *     ha xarxa o l'API està caiguda — així el panel és sempre demoable.
 *   - Resultats ordenats per CVSS descendent (els més greus primer).
 *
 * Documentació API: https://nvd.nist.gov/developers/vulnerabilities
 */
public class CveService {

    private static final String NVD_BASE =
        "https://services.nvd.nist.gov/rest/json/cves/2.0?resultsPerPage=10&keywordSearch=";
    private static final long CACHE_TTL_MS = 60L * 60 * 1000;
    private static final int HTTP_TIMEOUT = 6000;
    private static final int MAX_DESC = 240;

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private volatile boolean offlineMode = false;

    public void setOfflineMode(boolean offline) {
        this.offlineMode = offline;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    /**
     * Cerca CVEs per a un servei + versió. Si no es passa versió, cerca per
     * nom. Retorna llista buida si no hi ha resultats o si falla la consulta.
     */
    public List<CveEntry> lookup(String service, String version) {
        if (service == null || service.isBlank()) return List.of();
        String key = (service + "/" + (version == null ? "" : version)).toLowerCase();

        Cached c = cache.get(key);
        if (c != null && (System.currentTimeMillis() - c.timestamp) < CACHE_TTL_MS) {
            return c.entries;
        }

        List<CveEntry> entries;
        if (offlineMode) {
            entries = lookupOffline(service);
        } else {
            try {
                entries = lookupOnline(service, version);
            } catch (Exception e) {
                System.err.println(">>> [CVE] Fallback offline (motiu: " + e.getMessage() + ")");
                entries = lookupOffline(service);
            }
        }

        cache.put(key, new Cached(entries, System.currentTimeMillis()));
        return entries;
    }

    /**
     * Consulta l'API NVD i deserialitza la resposta.
     */
    private List<CveEntry> lookupOnline(String service, String version) throws IOException {
        StringBuilder q = new StringBuilder(service.trim());
        if (version != null && !version.isBlank()) q.append(' ').append(version.trim());
        String url = NVD_BASE + URLEncoder.encode(q.toString(), StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(HTTP_TIMEOUT);
        conn.setReadTimeout(HTTP_TIMEOUT);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "ShadowScan/1.2");

        int code = conn.getResponseCode();
        if (code != 200) {
            conn.disconnect();
            throw new IOException("HTTP " + code);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        conn.disconnect();

        return parseNvdResponse(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private List<CveEntry> parseNvdResponse(String json) {
        List<CveEntry> out = new ArrayList<>();
        Map<String, Object> root = JsonParser.parseObject(json);
        Object items = root.get("vulnerabilities");
        if (!(items instanceof List)) return out;

        for (Object item : (List<Object>) items) {
            try {
                Map<String, Object> cve = (Map<String, Object>) ((Map<String, Object>) item).get("cve");
                if (cve == null) continue;

                String id = String.valueOf(cve.get("id"));
                String desc = extractDescription(cve);
                double score = 0.0;
                String severity = "NONE";

                Object metrics = cve.get("metrics");
                if (metrics instanceof Map) {
                    double[] sev = extractCvss((Map<String, Object>) metrics);
                    score = sev[0];
                    severity = mapSeverity(score);
                }

                String url = "https://nvd.nist.gov/vuln/detail/" + id;
                out.add(new CveEntry(id, truncate(desc, MAX_DESC), score, severity, url));
            } catch (Exception e) {
                // Item malformat: l'ignorem i seguim amb la resta
            }
        }

        out.sort((a, b) -> Double.compare(b.getCvssScore(), a.getCvssScore()));
        return out;
    }

    @SuppressWarnings("unchecked")
    private String extractDescription(Map<String, Object> cve) {
        Object descs = cve.get("descriptions");
        if (descs instanceof List) {
            for (Object d : (List<Object>) descs) {
                if (d instanceof Map) {
                    Map<String, Object> dm = (Map<String, Object>) d;
                    if ("en".equals(dm.get("lang"))) {
                        Object v = dm.get("value");
                        if (v != null) return v.toString();
                    }
                }
            }
        }
        return "(sense descripció)";
    }

    /** Recorre v3.1 → v3.0 → v2 buscant la primera mètrica disponible. */
    @SuppressWarnings("unchecked")
    private double[] extractCvss(Map<String, Object> metrics) {
        for (String key : new String[]{"cvssMetricV31", "cvssMetricV30", "cvssMetricV2"}) {
            Object arr = metrics.get(key);
            if (arr instanceof List && !((List<Object>) arr).isEmpty()) {
                Map<String, Object> first = (Map<String, Object>) ((List<Object>) arr).get(0);
                Map<String, Object> data = (Map<String, Object>) first.get("cvssData");
                if (data != null && data.get("baseScore") instanceof Number) {
                    return new double[]{((Number) data.get("baseScore")).doubleValue()};
                }
            }
        }
        return new double[]{0.0};
    }

    private String mapSeverity(double score) {
        if (score >= 9.0) return "CRITICAL";
        if (score >= 7.0) return "HIGH";
        if (score >= 4.0) return "MEDIUM";
        if (score >  0.0) return "LOW";
        return "NONE";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /**
     * Catàleg offline reduït — només cobreix els serveis més comuns que
     * detecta ShadowScan via Nmap. Pensat per a demos sense xarxa o quan
     * NVD està rate-limited.
     */
    private List<CveEntry> lookupOffline(String service) {
        String s = service == null ? "" : service.toLowerCase();
        Map<String, List<CveEntry>> db = OFFLINE_DB;
        for (Map.Entry<String, List<CveEntry>> e : db.entrySet()) {
            if (s.contains(e.getKey())) {
                List<CveEntry> copy = new ArrayList<>(e.getValue());
                copy.sort((x, y) -> Double.compare(y.getCvssScore(), x.getCvssScore()));
                return copy;
            }
        }
        return Collections.emptyList();
    }

    private static final Map<String, List<CveEntry>> OFFLINE_DB = buildOfflineDb();

    private static Map<String, List<CveEntry>> buildOfflineDb() {
        Map<String, List<CveEntry>> m = new HashMap<>();
        m.put("openssh", List.of(
            new CveEntry("CVE-2024-6387", "regreSSHion: race condition en sshd que permet RCE no autenticat (versions 8.5p1–9.7p1).", 8.1, "HIGH",
                "https://nvd.nist.gov/vuln/detail/CVE-2024-6387"),
            new CveEntry("CVE-2023-38408", "OpenSSH ssh-agent permet RCE via PKCS#11 forwarding.", 9.8, "CRITICAL",
                "https://nvd.nist.gov/vuln/detail/CVE-2023-38408")
        ));
        m.put("apache", List.of(
            new CveEntry("CVE-2021-41773", "Apache HTTP Server 2.4.49 path traversal i RCE.", 9.8, "CRITICAL",
                "https://nvd.nist.gov/vuln/detail/CVE-2021-41773"),
            new CveEntry("CVE-2024-38476", "Apache 2.4 backend HTTP request smuggling.", 9.8, "CRITICAL",
                "https://nvd.nist.gov/vuln/detail/CVE-2024-38476")
        ));
        m.put("nginx", List.of(
            new CveEntry("CVE-2021-23017", "DNS resolver off-by-one a nginx, RCE possible.", 8.1, "HIGH",
                "https://nvd.nist.gov/vuln/detail/CVE-2021-23017")
        ));
        m.put("mysql", List.of(
            new CveEntry("CVE-2023-21980", "MySQL Server 5.7/8.0 — DoS no autenticat.", 7.5, "HIGH",
                "https://nvd.nist.gov/vuln/detail/CVE-2023-21980")
        ));
        m.put("smb", List.of(
            new CveEntry("CVE-2017-0144", "EternalBlue: SMBv1 RCE no autenticat.", 8.1, "HIGH",
                "https://nvd.nist.gov/vuln/detail/CVE-2017-0144"),
            new CveEntry("CVE-2020-0796", "SMBGhost: SMBv3 RCE en Windows 10.", 10.0, "CRITICAL",
                "https://nvd.nist.gov/vuln/detail/CVE-2020-0796")
        ));
        m.put("microsoft-ds", m.get("smb"));
        m.put("rdp", List.of(
            new CveEntry("CVE-2019-0708", "BlueKeep: RDP wormable RCE en Windows 7/Server 2008.", 9.8, "CRITICAL",
                "https://nvd.nist.gov/vuln/detail/CVE-2019-0708")
        ));
        m.put("ms-wbt-server", m.get("rdp"));
        m.put("vsftpd", List.of(
            new CveEntry("CVE-2011-2523", "vsftpd 2.3.4 backdoor: smiley-face → port 6200 shell.", 9.8, "CRITICAL",
                "https://nvd.nist.gov/vuln/detail/CVE-2011-2523")
        ));
        m.put("ftp", m.get("vsftpd"));
        m.put("telnet", List.of(
            new CveEntry("CVE-2020-10188", "telnetd buffer overflow → RCE no autenticat.", 9.8, "CRITICAL",
                "https://nvd.nist.gov/vuln/detail/CVE-2020-10188")
        ));
        return m;
    }

    private static final class Cached {
        final List<CveEntry> entries;
        final long timestamp;
        Cached(List<CveEntry> e, long t) { this.entries = e; this.timestamp = t; }
    }

    /** Per a la secció de stats al dashboard. */
    public Map<String, Object> stats() {
        Map<String, Object> s = new HashMap<>();
        s.put("entries_cached", cache.size());
        s.put("offline_mode", offlineMode);
        s.put("last_update", Instant.now().toString());
        return s;
    }
}
