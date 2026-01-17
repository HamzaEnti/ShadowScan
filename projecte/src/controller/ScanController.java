package controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import view.MainFrame;

public class ScanController {

    private ExecutorService pool; 
    private boolean enExecucio; 
    private MainFrame vista;
    private static final int NUM_THREADS = 20;

    public ScanController(MainFrame v) {
        this.vista = v;
        this.enExecucio = false;
    }

    public void escanearRang(String xarxa, PortScanMode mode) {
        // creem un pool nou cada vegada 
        pool = Executors.newFixedThreadPool(NUM_THREADS);
        enExecucio = true;

        System.out.println(">>> [SCAN] Iniciant escaneig de " + xarxa + "0/24");
        System.out.println(">>> [SCAN] Mode: " + mode.getDescripcio());

        // escanegem de .1 a .254 
        for (int i = 1; i <= 254; i++) {
            // si l'usuari ha dit d'aturar, parem
            if (!enExecucio) {
                System.out.println(">>> [SCAN] Escaneig aturat per l'usuari");
                break;
            }
            
            String ip = xarxa + i;
            // creem una tasca per cada IP i la posem a la cua
            pool.execute(new ScanTask(ip, vista, mode));
        }

        // shutdown graceful: no accepta mes tasques pero deixa acabar les que estan en curs
        pool.shutdown();
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
