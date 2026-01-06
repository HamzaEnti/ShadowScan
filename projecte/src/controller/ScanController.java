package controller;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import view.MainFrame; // importo la vista nova

public class ScanController {

    private ExecutorService pool;
    private boolean enExecucio; 
    private MainFrame vista; // guardo la referencia a la finestra

    // constructor modificat: ara rep la vista
    public ScanController(MainFrame v) {
        this.vista = v;
        this.enExecucio = false;
    }

    public void escanearRang(String xarxa, PortScanMode mode) {
    pool = Executors.newFixedThreadPool(20);
    enExecucio = true;

    for (int i = 1; i <= 254; i++) {
        if (!enExecucio) break;
        String ip = xarxa + i;
        pool.execute(new ScanTask(ip, vista, mode));
    }

    pool.shutdown();
}

    public void aturar() {
        System.out.println("Aturant escaneig.");
        enExecucio = false;
        if (pool != null) {
            pool.shutdownNow();
        }
    }
}