package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import model.ResultatHost;
import utils.NetworkUtil;
import view.MainFrame;

public class ScanTask implements Runnable {
    
   
    private String ip;
    private MainFrame vista;
    private PortScanMode mode;
    private static final int PORT_THREADS = 10;
    private static final int PORT_TIMEOUT = 50;

    public ScanTask(String ip, MainFrame v, PortScanMode mode) {
        this.ip = ip;
        this.vista = v;
        this.mode = mode;
    }

    @Override
    public void run() {
        // primer comprovem si la IP respon 
        // timeout de 200ms per no tardar massa
        if (!NetworkUtil.isReachable(ip, 200)) {
            // si no respon, no perdem temps escanejant ports
            return;
        }

        // la IP respon, creem un resultat
        ResultatHost host = new ResultatHost(ip);
        host.setEsViu(true);

        // llista thread-safe per guardar els ports que trobem
        // synchronized perque multiples threads hi escriuran alhora
        List<Integer> portsOberts = Collections.synchronizedList(new ArrayList<>());
        
        // Part feta amb IA
        ExecutorService portPool = Executors.newFixedThreadPool(PORT_THREADS);
    
        if (mode.esParcial()) {
            // mode parcial: nomes els ports comuns
            int[] ports = mode.getPorts();
            for (int port : ports) {
                portPool.execute(() -> {
                    if (NetworkUtil.isPortOpen(ip, port, PORT_TIMEOUT)) {
                        portsOberts.add(port);
                    }
                });
            }
        } else {
            // mode full: tots els 65535 ports
            for (int port = 1; port <= 65535; port++) {
                final int p = port;
                portPool.execute(() -> {
                    if (NetworkUtil.isPortOpen(ip, p, PORT_TIMEOUT / 2)) {
                        portsOberts.add(p);
                    }
                });
            }
        }
        // Fi part feta amb IA

        // esperem que acabin tots els escaneigs de ports
        portPool.shutdown();
        try {
            // donem fins a 10 minuts per escaneig full
            // per parcial sobra amb menys, pero no fa mal
            portPool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // si ens interrompen, no passa res, retornem el que tenim
            Thread.currentThread().interrupt();
        }

        // guardem els ports trobats al resultat
        host.setPortsOberts(portsOberts);

        // enviem el resultat a la UI
        // IMPORTANT: hem d'usar invokeLater perque estem en un thread secundari
        // i Swing no es thread-safe
        SwingUtilities.invokeLater(() -> vista.afegirResultat(host));
    }
}
