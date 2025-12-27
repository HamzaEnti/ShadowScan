package controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import view.MainFrame; // importo la vista nova


public class ScanController {

    private ExecutorService pool;
    private boolean enExecucio;
    private MainFrame vista; // guardo la referencia a la finestra
 

    public ScanController(MainFrame v) {
        this.vista = v;
        this.enExecucio = false;
    }

    // metode per arrencar tot
    public void escanearRang(String xarxa) {
        System.out.println("iniciant escaneig a la xarxa: " + xarxa);
        
        // neteja si hi ha fils penjats
        if (pool != null && !pool.isTerminated()) {
            pool.shutdownNow();
        }

        // creo 20 fils simultanis
        pool = Executors.newFixedThreadPool(20);
        enExecucio = true;

        // bucle per generar les 254 ips
        for (int i = 1; i <= 254; i++) {
            if (!enExecucio) break; 

            String ipObjectiu = xarxa + i;
            
            // li passo la ip a la tasca i la poso a la cua
            ScanTask tasca = new ScanTask(ipObjectiu, vista);
            pool.execute(tasca);
        }

        // tanco l'aixeta de tasques noves
        pool.shutdown();
    }

    // metode per parar de cop
    public void aturar() {
        System.out.println("aturant el proces...");
        enExecucio = false;
        if (pool != null) {
            pool.shutdownNow();
        }
    }
}