package distributed;

import controller.HostFoundListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import model.ResultatHost;
import utils.JsonParser;
import utils.JsonUtil;
import utils.NetworkUtil;

/**
 * Coordinator que reparteix un rang d'IPs entre N workers TCP.
 *
 * Algorisme: round-robin de les IPs entre els workers disponibles. Cada
 * worker rep un missatge de tipus 'scan' amb la seva tallada; els
 * resultats arriben com a missatges 'result' i s'agreguen a un llistat
 * compartit. El call a executeDistributed bloqueja fins que tots els
 * workers responen 'done'.
 */
public class ScanCoordinator {

    public static final class WorkerEndpoint {
        public final String host;
        public final int port;
        public final String token;
        public WorkerEndpoint(String host, int port, String token) {
            this.host = host; this.port = port; this.token = token;
        }
        @Override public String toString() { return host + ":" + port; }
    }

    private final List<WorkerEndpoint> workers;

    public ScanCoordinator(List<WorkerEndpoint> workers) {
        this.workers = new ArrayList<>(workers);
    }

    public List<WorkerEndpoint> getWorkers() {
        return new ArrayList<>(workers);
    }

    /**
     * Reparteix les IPs del rang entre els workers i recull els resultats.
     * Bloqueja fins que tots els workers han acabat.
     */
    public List<ResultatHost> executeDistributed(String startIp, String endIp,
                                                   String mode, boolean udp,
                                                   HostFoundListener listener) throws IOException {
        if (workers.isEmpty()) throw new IOException("No hi ha workers configurats");

        List<String> allIps = NetworkUtil.smartRange(startIp, endIp);
        if (allIps.isEmpty()) throw new IOException("Rang invàlid");

        // Repartim round-robin
        List<List<String>> shards = new ArrayList<>();
        for (int i = 0; i < workers.size(); i++) shards.add(new ArrayList<>());
        for (int i = 0; i < allIps.size(); i++) {
            shards.get(i % workers.size()).add(allIps.get(i));
        }

        List<ResultatHost> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(workers.size());
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(workers.size(), 8));

        for (int i = 0; i < workers.size(); i++) {
            WorkerEndpoint w = workers.get(i);
            List<String> shard = shards.get(i);
            if (shard.isEmpty()) { latch.countDown(); continue; }

            pool.execute(() -> {
                try {
                    runOnWorker(w, shard, mode, udp, results, listener);
                } catch (Exception e) {
                    System.err.println(">>> [COORD] Worker " + w + " ha fallat: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Coordinator interromput");
        }
        pool.shutdown();

        return new ArrayList<>(results);
    }

    @SuppressWarnings("unchecked")
    private void runOnWorker(WorkerEndpoint w, List<String> shard,
                             String mode, boolean udp,
                             List<ResultatHost> sharedResults,
                             HostFoundListener listener) throws IOException {
        try (Socket s = new Socket(w.host, w.port);
             PrintWriter out = new PrintWriter(s.getOutputStream(), false, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {

            // Construim missatge "scan"
            StringBuilder ips = new StringBuilder("[");
            for (int i = 0; i < shard.size(); i++) {
                if (i > 0) ips.append(",");
                ips.append("\"").append(JsonUtil.escape(shard.get(i))).append("\"");
            }
            ips.append("]");

            String tokenField = (w.token != null && !w.token.isBlank())
                ? ",\"token\":\"" + JsonUtil.escape(w.token) + "\"" : "";

            String msg = "{\"type\":\"scan\",\"ips\":" + ips
                + ",\"mode\":\"" + JsonUtil.escape(mode) + "\""
                + ",\"udp\":" + udp + tokenField + "}";

            out.println(msg);
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                Map<String, Object> r;
                try { r = JsonParser.parseObject(line); }
                catch (Exception e) { continue; }

                String type = String.valueOf(r.get("type"));
                if ("done".equals(type)) return;
                if ("error".equals(type)) {
                    System.err.println(">>> [COORD] Worker error: " + r.get("message"));
                    return;
                }
                if ("result".equals(type)) {
                    Object hostObj = r.get("host");
                    if (hostObj instanceof Map) {
                        ResultatHost h = hostFromMap((Map<String, Object>) hostObj);
                        synchronized (sharedResults) { sharedResults.add(h); }
                        if (listener != null) listener.onHostFound(h);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ResultatHost hostFromMap(Map<String, Object> m) {
        ResultatHost h = new ResultatHost(String.valueOf(m.get("ip")));
        Object alive = m.get("esViu");
        h.setEsViu(Boolean.TRUE.equals(alive));
        Object hostname = m.get("hostname");
        if (hostname != null) h.setHostname(hostname.toString());
        Object ports = m.get("ports");
        if (ports instanceof List) {
            List<Integer> ip = new ArrayList<>();
            for (Object v : (List<Object>) ports) {
                if (v instanceof Number) ip.add(((Number) v).intValue());
            }
            h.setPortsOberts(ip);
        }
        return h;
    }
}
