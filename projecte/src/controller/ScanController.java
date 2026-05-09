package controller;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import model.ResultatHost;
import utils.NetworkUtil;

/**
 * Orquestra l'escaneig d'un rang d'IPs.
 *
 * Ja no depèn de MainFrame: la comunicació amb la vista es fa via
 * {@link HostFoundListener} (per a cada host trobat) i {@link ScanCallback}
 * (al finalitzar). Així el controlador és testejable sense Swing.
 */
public class ScanController {

    private ExecutorService pool;
    private volatile boolean enExecucio;
    private final HostFoundListener hostListener;
    private final AtomicInteger hostsFound = new AtomicInteger(0);
    private static final int NUM_THREADS = 20;

    public interface ScanCallback {
        void onScanFinished(int hostsFound);
    }

    private ScanCallback callback;

    public ScanController(HostFoundListener hostListener) {
        this.hostListener = hostListener;
        this.enExecucio = false;
    }

    public void setCallback(ScanCallback cb) {
        this.callback = cb;
    }

    /**
     * Compatibilitat amb el codi previ: prefix "192.168.1." → escaneja .1-.254.
     */
    public void escanearRang(String xarxa, PortScanMode mode) {
        String start = xarxa + "1";
        String end   = xarxa + "254";
        escanearRang(start, end, mode);
    }

    /**
     * Versió generalitzada: accepta qualsevol rang IPv4 (ex: 10.0.0.5 → 10.0.0.50,
     * o subxarxes /16 senceres). Usa NetworkUtil.rangIps per validar.
     */
    public void escanearRang(String startIp, String endIp, PortScanMode mode) {
        List<String> ips = NetworkUtil.rangIps(startIp, endIp);
        if (ips.isEmpty()) {
            System.err.println(">>> [SCAN] Rang invàlid: " + startIp + " → " + endIp);
            if (callback != null) callback.onScanFinished(0);
            return;
        }

        pool = Executors.newFixedThreadPool(NUM_THREADS);
        enExecucio = true;
        hostsFound.set(0);

        System.out.println(">>> [SCAN] Iniciant escaneig de " + startIp + " → " + endIp
                + " (" + ips.size() + " IPs)");
        System.out.println(">>> [SCAN] Mode: " + mode.getDescripcio());

        // Embolicar el listener per comptar hosts trobats sense que la
        // vista s'hagi de preocupar del recompte.
        HostFoundListener counting = (ResultatHost h) -> {
            hostsFound.incrementAndGet();
            if (hostListener != null) hostListener.onHostFound(h);
        };

        for (String ip : ips) {
            if (!enExecucio) {
                System.out.println(">>> [SCAN] Escaneig aturat per l'usuari");
                break;
            }
            pool.execute(new ScanTask(ip, counting, mode));
        }

        pool.shutdown();

        // Thread de monitoring que notifica el final passant el comptador real.
        new Thread(() -> {
            try {
                pool.awaitTermination(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            enExecucio = false;
            if (callback != null) {
                callback.onScanFinished(hostsFound.get());
            }
        }, "scan-monitor").start();
    }

    public void aturar() {
        System.out.println(">>> [SCAN] Aturant escaneig...");
        enExecucio = false;
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    public boolean isEnExecucio() {
        return enExecucio;
    }

    public int getHostsFound() {
        return hostsFound.get();
    }
}
