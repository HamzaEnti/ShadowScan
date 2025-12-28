package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class FuzzingService {

    public void lanzarFuzzing(String ip, int port, String rutaWordlist) {
        
        if (!new File(rutaWordlist).exists()) {
            System.out.println("Error: La wordlist de fuzzing no existeix: " + rutaWordlist);
            return;
        }

        System.out.println("Arrancant FFUF Professional...");
        String url = "http://" + ip + ":" + port + "/FUZZ";
        
        System.out.println("Target: " + url);
        System.out.println("Wordlist: " + rutaWordlist);

        try {
            // Comanda escalable: ffuf llegeix directament del disc dur
            // -c (color), -mc (match codes), -s (silent mode)
            ProcessBuilder pb = new ProcessBuilder(
                "ffuf",
                "-w", rutaWordlist,
                "-u", url,
                "-mc", "200,301,302,403",
                "-c",
                "-s" 
            );

            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String linia;
            
            System.out.println("Escanejant directoris...");
            
            while ((linia = reader.readLine()) != null) {
                // FFUF en mode silent nomes imprimeix els resultats bons
                System.out.println(" Ruta descoberta: /" + linia);
            }
            
            p.waitFor();
            System.out.println("Fuzzing completat.");

        } catch (Exception e) {
            System.out.println("Error executant ffuf: " + e.getMessage());
        }
    }
}