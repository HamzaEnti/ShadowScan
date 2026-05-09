package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import controller.HostFoundListener;
import controller.PortScanMode;
import controller.ScanController;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import model.CveEntry;
import model.ResultatHost;
import model.ScanProfile;
import services.CveService;
import utils.JsonExporter;
import utils.JsonParser;
import utils.JsonUtil;
import utils.ProfileStore;

/**
 * Servidor REST minimalista per integrar ShadowScan amb pipelines externes.
 *
 * Endpoints:
 *   GET  /api/health                — ping bàsic
 *   GET  /api/profiles              — llista de perfils desats
 *   POST /api/profiles              — desa un nou perfil (JSON body)
 *   POST /api/scans                 — inicia escaneig (body: {start, end, mode, udp?})
 *   GET  /api/scans                 — llista d'execucions
 *   GET  /api/scans/{id}            — detall d'una execució + hosts trobats
 *   GET  /api/scans/{id}/topology   — exporta topologia RedTrace
 *   GET  /api/cve?service=X         — consulta CVEs
 *
 * Autenticació: bearer token simple via header "Authorization: Bearer &lt;token&gt;"
 * si es passa al constructor (opcional, perfecte per a localhost).
 */
public class RestApi {

    private final int port;
    private final String token;
    private final ProfileStore profiles = new ProfileStore();
    private final CveService cveService = new CveService();
    private final Map<Long, ScanRun> runs = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private HttpServer server;

    public RestApi(int port, String token) {
        this.port = port;
        this.token = (token == null || token.isBlank()) ? null : token.trim();
    }

    public synchronized void start() throws IOException {
        if (server != null) return;
        server = HttpServer.create(new InetSocketAddress(port), 16);
        server.setExecutor(Executors.newFixedThreadPool(4));

        register("/api/health",   this::handleHealth);
        register("/api/profiles", this::handleProfiles);
        register("/api/scans",    this::handleScans);
        register("/api/cve",      this::handleCve);

        server.start();
        System.out.println(">>> [REST] Servidor escoltant a http://localhost:" + port + "/api/");
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println(">>> [REST] Aturat");
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    public int getPort() {
        return port;
    }

    /* ─── Routing ─────────────────────────────────────────────────────── */

    private interface Handler { void handle(HttpExchange e) throws IOException; }

    private void register(String prefix, Handler h) {
        server.createContext(prefix, ex -> {
            try {
                if (!authorize(ex)) { send(ex, 401, json("{\"error\":\"unauthorized\"}")); return; }
                h.handle(ex);
            } catch (IllegalArgumentException iae) {
                send(ex, 400, json("{\"error\":\"" + JsonUtil.escape(iae.getMessage()) + "\"}"));
            } catch (Exception other) {
                other.printStackTrace();
                send(ex, 500, json("{\"error\":\"" + JsonUtil.escape(other.getMessage()) + "\"}"));
            }
        });
    }

    private boolean authorize(HttpExchange ex) {
        if (token == null) return true;
        String hdr = ex.getRequestHeaders().getFirst("Authorization");
        return hdr != null && hdr.equals("Bearer " + token);
    }

    /* ─── Handlers ────────────────────────────────────────────────────── */

    private void handleHealth(HttpExchange ex) throws IOException {
        send(ex, 200, json("{\"status\":\"ok\",\"version\":\"2.0\",\"runs\":" + runs.size() + "}"));
    }

    private void handleProfiles(HttpExchange ex) throws IOException {
        switch (ex.getRequestMethod()) {
            case "GET": {
                List<ScanProfile> all = profiles.loadAll();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < all.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(all.get(i).toJson());
                }
                sb.append("]");
                send(ex, 200, json(sb.toString()));
                break;
            }
            case "POST": {
                String body = readBody(ex);
                ScanProfile p = ScanProfile.fromJson(body);
                profiles.save(p);
                send(ex, 201, json(p.toJson()));
                break;
            }
            default:
                send(ex, 405, json("{\"error\":\"method not allowed\"}"));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleScans(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");
        // /api/scans         → parts = ["", "api", "scans"]
        // /api/scans/12      → parts = ["", "api", "scans", "12"]
        // /api/scans/12/topology

        if ("POST".equals(ex.getRequestMethod()) && parts.length == 3) {
            Map<String, Object> body = JsonParser.parseObject(readBody(ex));
            String start = String.valueOf(body.get("start"));
            String end   = String.valueOf(body.get("end"));
            String mode  = String.valueOf(body.getOrDefault("mode", "PARCIAL"));
            boolean udp  = Boolean.TRUE.equals(body.get("udp"));

            long id = nextId.getAndIncrement();
            ScanRun run = new ScanRun(id, start, end, mode, udp);
            runs.put(id, run);

            // Llançem en thread separat per no bloquejar la resposta
            new Thread(() -> run.execute(), "rest-scan-" + id).start();

            send(ex, 202, json("{\"id\":" + id + ",\"status\":\"started\"}"));
            return;
        }

        if ("GET".equals(ex.getRequestMethod()) && parts.length == 3) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (ScanRun r : runs.values()) {
                if (!first) sb.append(",");
                sb.append(r.summaryJson());
                first = false;
            }
            sb.append("]");
            send(ex, 200, json(sb.toString()));
            return;
        }

        if ("GET".equals(ex.getRequestMethod()) && parts.length >= 4) {
            long id;
            try { id = Long.parseLong(parts[3]); }
            catch (NumberFormatException nfe) { send(ex, 400, json("{\"error\":\"invalid id\"}")); return; }

            ScanRun run = runs.get(id);
            if (run == null) { send(ex, 404, json("{\"error\":\"not found\"}")); return; }

            if (parts.length == 5 && "topology".equals(parts[4])) {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"nodes\":[");
                List<ResultatHost> hosts = run.hosts();
                for (int i = 0; i < hosts.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(hosts.get(i).toJson());
                }
                sb.append("]}");
                send(ex, 200, json(sb.toString()));
                return;
            }

            send(ex, 200, json(run.detailJson()));
            return;
        }

        send(ex, 404, json("{\"error\":\"not found\"}"));
    }

    private void handleCve(HttpExchange ex) throws IOException {
        String q = ex.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(q);
        String service = params.get("service");
        if (service == null || service.isBlank()) {
            send(ex, 400, json("{\"error\":\"service param required\"}"));
            return;
        }
        cveService.setOfflineMode("true".equals(params.getOrDefault("offline", "false")));
        List<CveEntry> entries = cveService.lookup(service, params.get("version"));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(entries.get(i).toJson());
        }
        sb.append("]");
        send(ex, 200, json(sb.toString()));
    }

    /* ─── Helpers ─────────────────────────────────────────────────────── */

    private static void send(HttpExchange ex, int code, byte[] body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private static byte[] json(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(String q) {
        Map<String, String> m = new HashMap<>();
        if (q == null || q.isEmpty()) return m;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String k = java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            m.put(k, v);
        }
        return m;
    }

    /* ─── ScanRun: estat d'un escaneig llançat per l'API ──────────────── */

    private static final class ScanRun {
        final long id;
        final String startIp, endIp, mode;
        final boolean udp;
        final List<ResultatHost> hosts = Collections.synchronizedList(new ArrayList<>());
        volatile String status = "queued";
        volatile long startedAt;
        volatile long finishedAt;

        ScanRun(long id, String s, String e, String m, boolean udp) {
            this.id = id; this.startIp = s; this.endIp = e; this.mode = m; this.udp = udp;
        }

        void execute() {
            startedAt = System.currentTimeMillis();
            status = "running";
            try {
                HostFoundListener l = hosts::add;
                ScanController c = new ScanController(l);
                final Object lock = new Object();
                c.setCallback(n -> { synchronized (lock) { lock.notifyAll(); } });
                PortScanMode m = "FULL".equals(mode) ? PortScanMode.FULL : PortScanMode.PARCIAL;
                c.escanearRang(startIp, endIp, m, udp);
                synchronized (lock) {
                    while (c.isEnExecucio()) lock.wait();
                }
                status = "finished";
            } catch (Exception ex) {
                status = "error";
            }
            finishedAt = System.currentTimeMillis();
        }

        List<ResultatHost> hosts() { return new ArrayList<>(hosts); }

        String summaryJson() {
            return "{\"id\":" + id + ",\"status\":\"" + status + "\",\"start\":\""
                + JsonUtil.escape(startIp) + "\",\"end\":\"" + JsonUtil.escape(endIp)
                + "\",\"mode\":\"" + JsonUtil.escape(mode) + "\",\"udp\":" + udp
                + ",\"hosts_found\":" + hosts.size() + "}";
        }

        String detailJson() {
            String hostsJson = hosts().stream()
                .map(ResultatHost::toJson)
                .collect(Collectors.joining(","));
            return "{\"id\":" + id + ",\"status\":\"" + status + "\","
                + "\"start\":\"" + JsonUtil.escape(startIp) + "\","
                + "\"end\":\"" + JsonUtil.escape(endIp) + "\","
                + "\"mode\":\"" + JsonUtil.escape(mode) + "\","
                + "\"udp\":" + udp + ","
                + "\"started_at\":" + startedAt + ","
                + "\"finished_at\":" + finishedAt + ","
                + "\"hosts\":[" + hostsJson + "]}";
        }
    }

    // Per a integració amb el `JsonExporter` ja existent (no usat directament aquí
    // però el mantenim importat com a documentació de l'API alternativa).
    @SuppressWarnings("unused")
    private void exportRedTraceCompat(List<ResultatHost> hosts, String path) {
        JsonExporter.saveToTopology(hosts, path);
    }
}
