package distributed;

import controller.HostFoundListener;
import controller.PortScanMode;
import controller.ScanTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import model.ResultatHost;
import utils.JsonParser;

/**
 * Worker que rep llistes d'IPs del coordinator i les escaneja localment.
 *
 * Disseny: un sol worker per procés/host, escolta TCP. La seguretat és
 * intencionalment senzilla (token compartit) — pensat per a xarxes de
 * confiança o tunelitzat per SSH/VPN.
 */
public class ScanWorker {

    private final int port;
    private final String token;
    private ServerSocket server;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread acceptThread;

    public ScanWorker(int port, String token) {
        this.port = port;
        this.token = token;
    }

    public synchronized void start() throws IOException {
        if (running.get()) return;
        server = new ServerSocket(port);
        running.set(true);
        acceptThread = new Thread(this::acceptLoop, "scan-worker-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        System.out.println(">>> [WORKER] Escoltant a port " + port);
    }

    public synchronized void stop() {
        running.set(false);
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        System.out.println(">>> [WORKER] Aturat");
    }

    public boolean isRunning() { return running.get(); }
    public int getPort() { return port; }

    private void acceptLoop() {
        ExecutorService pool = Executors.newCachedThreadPool();
        while (running.get()) {
            try {
                Socket client = server.accept();
                pool.execute(() -> handleClient(client));
            } catch (IOException e) {
                if (running.get()) System.err.println(">>> [WORKER] Accept error: " + e.getMessage());
            }
        }
        pool.shutdown();
        try { pool.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
    }

    private void handleClient(Socket s) {
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), false, StandardCharsets.UTF_8)) {

            String line;
            while ((line = in.readLine()) != null) {
                Map<String, Object> msg;
                try { msg = JsonParser.parseObject(line); }
                catch (Exception e) {
                    out.println("{\"type\":\"error\",\"message\":\"json invalid\"}");
                    out.flush();
                    continue;
                }

                String authToken = (String) msg.get("token");
                if (token != null && !token.equals(authToken)) {
                    out.println("{\"type\":\"error\",\"message\":\"unauthorized\"}");
                    out.flush();
                    return;
                }

                String type = String.valueOf(msg.get("type"));
                switch (type) {
                    case "ping":
                        out.println("{\"type\":\"pong\"}");
                        out.flush();
                        break;

                    case "scan":
                        runScan(msg, out);
                        break;

                    default:
                        out.println("{\"type\":\"error\",\"message\":\"unknown type\"}");
                        out.flush();
                }
            }
        } catch (IOException e) {
            // Client desconnectat, no és greu
        }
    }

    @SuppressWarnings("unchecked")
    private void runScan(Map<String, Object> msg, PrintWriter out) {
        Object ipsObj = msg.get("ips");
        if (!(ipsObj instanceof List)) {
            out.println("{\"type\":\"error\",\"message\":\"missing ips\"}");
            out.flush();
            return;
        }
        List<Object> ips = (List<Object>) ipsObj;
        PortScanMode mode = "FULL".equals(msg.get("mode")) ? PortScanMode.FULL : PortScanMode.PARCIAL;
        boolean udp = Boolean.TRUE.equals(msg.get("udp"));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        HostFoundListener listener = (ResultatHost h) -> {
            synchronized (out) {
                out.println("{\"type\":\"result\",\"host\":" + h.toJson() + "}");
                out.flush();
            }
        };

        for (Object ip : ips) {
            if (ip == null) continue;
            pool.execute(new controller.ScanTask(ip.toString(), listener, mode, udp));
        }
        pool.shutdown();
        try { pool.awaitTermination(30, TimeUnit.MINUTES); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        synchronized (out) {
            out.println("{\"type\":\"done\"}");
            out.flush();
        }
    }

    /** Punt d'entrada per executar un worker autònom: java -cp ... distributed.ScanWorker [port] [token] */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        String token = args.length > 1 ? args[1] : null;
        ScanWorker w = new ScanWorker(port, token);
        w.start();
        System.out.println(">>> [WORKER] Prem Ctrl+C per aturar.");
        Thread.currentThread().join();
    }
}
