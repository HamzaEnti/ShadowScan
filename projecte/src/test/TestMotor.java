/*
package test;
import java.net.InetAddress; // Per obtenir la IP local

import controller.ScanController;

public class TestMotor {

    public static void main(String[] args) {
        ScanController control = new ScanController();
        String xarxaLocal = "";

        try {
            // 1. Obtenim la IP de la maquina actual
            InetAddress local = InetAddress.getLocalHost();
            String laMevaIp = local.getHostAddress();
            
            System.out.println("La meva IP detectada és: " + laMevaIp);

            // 2. Tallem l'últim número per obtenir la xarxa (ex: "172.20.10.12" -> "172.20.10.")
            // Busquem l'últim punt i agafem tot el que hi ha abans, incloent el punt
            xarxaLocal = laMevaIp.substring(0, laMevaIp.lastIndexOf(".") + 1);

            System.out.println("--- Test de Logica Backend ---");
            System.out.println("Escanejant automàticament la xarxa: " + xarxaLocal + "X");

            // 3. Iniciem l'escaneig amb la xarxa detectada
            control.escanearRang(xarxaLocal);

            // 4. Esperem (30s)
            System.out.println("Esperant resultats (30 segons)...");
            Thread.sleep(30000); 

            control.aturar();

        } catch (Exception e) {
            System.out.println("Error detectant la IP o durant el test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
*/