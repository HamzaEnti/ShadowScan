package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import model.ResultatHost;
import utils.NetworkUtil;

/**
 * Tasca d'escaneig per a un sol host.
 *
 * Notifica el resultat via {@link HostFoundListener} en lloc de tenir una
 * referència directa a la vista — això permet testejar la classe sense
 * Swing i compleix la separació MVC.
 */
public class ScanTask implements Runnable {

    private final String ip;
    private final HostFoundListener listener;
    private final PortScanMode mode;
    private final boolean udpScan;

    private static final int PORT_THREADS = 50;
    private static final int PORT_TIMEOUT = 50;
    private static final int UDP_TIMEOUT = 600;
    // Backpressure: limita la cua d'objectes pendents tant en mode FULL com PARCIAL
    private static final int MAX_QUEUED = 500;
    private static final int ICMP_TIMEOUT = 200;
    private static final int TCP_FALLBACK_TIMEOUT = 150;

    /** Ports UDP comuns escanejats si udpScan == true. Curt per cost. */
    private static final int[] UDP_PORTS = {53, 67, 69, 123, 137, 138, 161, 500, 514, 1900, 5353};

    public ScanTask(String ip, HostFoundListener listener, PortScanMode mode) {
        this(ip, listener, mode, false);
    }

    public ScanTask(String ip, HostFoundListener listener, PortScanMode mode, boolean udpScan) {
        this.ip = ip;
        this.listener = listener;
        this.mode = mode;
        this.udpScan = udpScan;
    }

    @Override
    public void run() {
        // FIX crític: si ICMP falla provem TCP a ports comuns. Sense això,
        // hosts amb firewall o sense privilegis ICMP es perdien encara que
        // tinguessin serveis oberts.
        if (!NetworkUtil.isHostAlive(ip, ICMP_TIMEOUT, TCP_FALLBACK_TIMEOUT)) {
            return;
        }

        ResultatHost host = new ResultatHost(ip);
        host.setEsViu(true);

        List<Integer> portsOberts = Collections.synchronizedList(new ArrayList<>());

        ExecutorService portPool = Executors.newFixedThreadPool(PORT_THREADS);
        // FIX: també apliquem el semàfor en mode parcial per coherència i per
        // evitar pics de càrrega quan en el futur s'ampliï la llista de ports.
        Semaphore sem = new Semaphore(MAX_QUEUED);

        try {
            int[] ports = mode.esParcial() ? mode.getPorts() : null;
            int total = (ports != null) ? ports.length : 65535;

            for (int idx = 0; idx < total; idx++) {
                final int p = (ports != null) ? ports[idx] : (idx + 1);
                try {
                    sem.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                portPool.execute(() -> {
                    try {
                        int timeout = mode.esParcial() ? PORT_TIMEOUT : PORT_TIMEOUT / 2;
                        if (NetworkUtil.isPortOpen(ip, p, timeout)) {
                            portsOberts.add(p);
                        }
                    } finally {
                        sem.release();
                    }
                });
            }
        } finally {
            portPool.shutdown();
            try {
                portPool.awaitTermination(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // UDP scan opcional — probes a un conjunt curt de ports comuns
        if (udpScan) {
            for (int up : UDP_PORTS) {
                if (NetworkUtil.isUdpPortRespondingStrict(ip, up, UDP_TIMEOUT)) {
                    portsOberts.add(up);
                }
            }
        }

        host.setPortsOberts(portsOberts);

        // Resolució de hostname després d'haver fet l'escaneig de ports —
        // si falla, ResultatHost simplement queda amb hostname = null.
        try {
            String hn = NetworkUtil.resolveHostname(ip);
            if (hn != null && !hn.equals(ip)) {
                host.setHostname(hn);
            }
        } catch (Exception ignored) {}

        if (listener != null) {
            listener.onHostFound(host);
        }
    }
}
