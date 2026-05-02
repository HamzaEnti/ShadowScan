package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import model.ResultatHost;
import utils.NetworkUtil;
import view.MainFrame;

public class ScanTask implements Runnable {

    private String ip;
    private MainFrame vista;
    private PortScanMode mode;
    private static final int PORT_THREADS = 50;
    private static final int PORT_TIMEOUT = 50;
    // FIX: limitem les tasques en cua per evitar OutOfMemoryError en mode FULL
    // Sense això, mode FULL encolava 65535 Runnable per host × 20 hosts = ~1.3M objectes en RAM
    // Assisted by Claude (Anthropic) — semaphore-based backpressure for full port scan
    private static final int MAX_QUEUED = 500;

    public ScanTask(String ip, MainFrame v, PortScanMode mode) {
        this.ip = ip;
        this.vista = v;
        this.mode = mode;
    }

    @Override
    public void run() {
        if (!NetworkUtil.isReachable(ip, 200)) {
            return;
        }

        ResultatHost host = new ResultatHost(ip);
        host.setEsViu(true);

        List<Integer> portsOberts = Collections.synchronizedList(new ArrayList<>());

        ExecutorService portPool = Executors.newFixedThreadPool(PORT_THREADS);

        if (mode.esParcial()) {
            // mode parcial: ports comuns — no hi ha risc de memoria
            int[] ports = mode.getPorts();
            for (int port : ports) {
                portPool.execute(() -> {
                    if (NetworkUtil.isPortOpen(ip, port, PORT_TIMEOUT)) {
                        portsOberts.add(port);
                    }
                });
            }
        } else {
            // FIX mode FULL: usem semàfor per limitar les tasques en cua
            // Sense això eren 65535 Runnable encolats per host → OOM possible
            Semaphore sem = new Semaphore(MAX_QUEUED);
            for (int port = 1; port <= 65535; port++) {
                final int p = port;
                try {
                    sem.acquire(); // bloqueja si la cua és plena
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                portPool.execute(() -> {
                    try {
                        if (NetworkUtil.isPortOpen(ip, p, PORT_TIMEOUT / 2)) {
                            portsOberts.add(p);
                        }
                    } finally {
                        sem.release(); // allibera espai a la cua
                    }
                });
            }
        }

        portPool.shutdown();
        try {
            portPool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        host.setPortsOberts(portsOberts);

        SwingUtilities.invokeLater(() -> vista.afegirResultat(host));
    }
}
