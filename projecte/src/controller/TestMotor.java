package controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// CLASSE DE PROVES - NO es part del programa final
// Serveix per provar els Threads sense tenir la interfície de l'Oscar encara
public class TestMotor {

    public static void main(String[] args) {
        
        // Dades de prova (Canviar segons on estigui connectat)
        String xarxa = "192.168.1."; 
        int inici = 1;
        int fi = 254;

        System.out.println("Començant test de logica");
        System.out.println("Escanejant rang: " + xarxa + inici + " fins a " + fi);

        // Creem el pool de 20 fils com demana l'enunciat
        ExecutorService executor = Executors.newFixedThreadPool(20);
        ScannerService scanner = new ScannerService();

        long start = System.currentTimeMillis();

        // Bucle per generar les tasques
        for (int i = inici; i <= fi; i++) {
            String ipActual = xarxa + i;

            // Aixo s'executa en paral·lel
            executor.submit(() -> {
                // Primer mirem si respon al ping
                if (scanner.isReachable(ipActual)) {
                    System.out.println("IP TROBADA: " + ipActual);
                    
                    // Si respon, mirem ports
                    var ports = scanner.escanejarPortsComuns(ipActual);
                    if (!ports.isEmpty()) {
                        System.out.println(" Ports oberts a " + ipActual + ": " + ports);
                    }
                } 
                // Si no respon no fem print per no embrutar la consola
            });
        }

        // Tanquem el xiringuito de fils
        executor.shutdown();
        try {
            // Esperem un màxim de 10s a que acabin tots
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        System.out.println("Fi del test. Temps total: " + (end - start) + "ms");
    }
}