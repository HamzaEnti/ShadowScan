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

    public void escanearRang(String xarxa) {
        System.out.println("iniciant escaneig a: " + xarxa);
        
        if (pool != null && !pool.isTerminated()) {
            pool.shutdownNow();
        }

        pool = Executors.newFixedThreadPool(20);
        enExecucio = true;

        for (int i = 1; i <= 254; i++) {
            if (!enExecucio) break; 

            String ipObjectiu = xarxa + i;
            
            // AQUI EL CANVI IMPORTANT:
            // passo la vista a la tasca perque pugui pintar la taula
            ScanTask tasca = new ScanTask(ipObjectiu, vista);
            pool.execute(tasca);
        }

        pool.shutdown();
    }

    public void aturar() {
        System.out.println("aturant...");
        enExecucio = false;
        if (pool != null) {
            pool.shutdownNow();
        }
    }
}