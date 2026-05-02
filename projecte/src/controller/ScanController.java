package controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import view.MainFrame;

public class ScanController {

    private ExecutorService pool;
    // FIX: volatile per garantir visibilitat entre threads (abans era plain boolean → race condition)
    private volatile boolean enExecucio;
    private MainFrame vista;
    private static final int NUM_THREADS = 20;

    // Callback opcional per notificar quan l'escaneig acaba
    // Assisted by Claude (Anthropic) — callback pattern for scan completion UI update
    public interface ScanCallback {
        void onScanFinished(int hostsFound);
    }

    private ScanCallback callback;

    public ScanController(MainFrame v) {
        this.vista = v;
        this.enExecucio = false;
    }

    public void setCallback(ScanCallback cb) {
        this.callback = cb;
    }

    public void escanearRang(String xarxa, PortScanMode mode) {
        pool = Executors.newFixedThreadPool(NUM_THREADS);
        enExecucio = true;

        System.out.println(">>> [SCAN] Iniciant escaneig de " + xarxa + "0/24");
        System.out.println(">>> [SCAN] Mode: " + mode.getDescripcio());

        for (int i = 1; i <= 254; i++) {
            if (!enExecucio) {
                System.out.println(">>> [SCAN] Escaneig aturat per l'usuari");
                break;
            }
            String ip = xarxa + i;
            pool.execute(new ScanTask(ip, vista, mode));
        }

        pool.shutdown();

        // FIX: notifiquem quan acaba via thread de monitoring, no bloquejem la UI
        new Thread(() -> {
            try {
                pool.awaitTermination(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            enExecucio = false;
            if (callback != null) {
                // el callback s'executa des del thread de monitoring,
                // els components Swing l'han d'usar amb invokeLater
                callback.onScanFinished(0);
            }
        }).start();
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
}
