package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FuzzingService {
    private List<String> foundUrls;
    public FuzzingService() {
        // Llista buida per guardar resultats
        this.foundUrls = new ArrayList<>();
    }

    public boolean checkInstalled() {
        try {
            // Comprova si ffuf està instal·lat
            ProcessBuilder pb = new ProcessBuilder("ffuf", "-version");
            Process p = pb.start();
            return (p.waitFor() == 0);
        } catch (Exception e) {
            // Si falla, no està instal·lat
            return false;
        }
    }

    public void lanzarFuzzing(String ip, int port, String rutaWordlist) {
        // Comprova que la wordlist existeix
        File wl = new File(rutaWordlist);
        if (!wl.exists()) {
            System.err.println(">>> [ERROR] Wordlist no trobada: " + rutaWordlist);
            return;
        }

        // URL amb FUZZ per substituir
        String url = "http://" + ip + ":" + port + "/FUZZ";
        System.out.println(">>> [FFUF] Fuzzing a: " + url);

        try {
            // Sense '-c' per evitar caràcters rars
            // Sense '-s' per veure el banner
            ProcessBuilder pb = new ProcessBuilder(
                "ffuf", 
                "-w", rutaWordlist, 
                "-u", url, 
                "-mc", "200,301,302,403"
            );

            pb.redirectErrorStream(true); // Junta stdout i stderr
            Process p = pb.start();

            // Llegeix la sortida en temps real
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                // Mostra cada línia
                System.out.println("[FFUF] " + line);
            }

            // Espera que acabi
            p.waitFor();
            System.out.println(">>> [FFUF] Finalitzat.");

        } catch (Exception e) {
            // Error general
            System.err.println(">>> [ERROR] Ffuf error: " + e.getMessage());
        }
    }

    // Exporta resultats a CSV
    public boolean exportReportToCSV(File file) {
        if (foundUrls.isEmpty()) {
            System.out.println("No hi ha resultats per exportar.");
            return false;
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("URL,DATA\n");
            for (String s : foundUrls) {
                writer.write("\"" + s + "\",\"" + new java.util.Date() + "\"\n");
            }
            System.out.println("CSV Exportat: " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
