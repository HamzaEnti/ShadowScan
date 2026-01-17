package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class NmapWrapper {
    private List<String> foundUrls;
    public NmapWrapper() {
        // Llista buida per guardar resultats
        this.foundUrls = new ArrayList<>();
    }

    // Comprova si nmap està instal·lat
    public boolean checkNmapInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("nmap", "--version");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return (exitCode == 0);
        } catch (Exception e) {
            // Si falla, no està instal·lat
            return false;
        }
    }

    // Escaneig principal amb nmap
    public void escanearConNmap(String ip) {
        System.out.println(">>> [NMAP SERVICE] Llançant comanda contra: " + ip);

        try {
            // Comanda bàsica amb detecció de versions (-sV) i velocitat (-T4)
            ProcessBuilder pb = new ProcessBuilder("nmap", "-sV", "-T4", ip);

            pb.redirectErrorStream(true); // Junta stdout i stderr

            Process process = pb.start();

            // Llegeix la sortida en temps real
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                // Mostra cada línia al log
                System.out.println(line);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println(">>> [NMAP] Escaneig finalitzat amb èxit.");
            } else {
                System.err.println(">>> [NMAP] Ha acabat amb errors (Codi: " + exitCode + ")");
            }

        } catch (Exception e) {
            // Error general
            System.err.println(">>> [ERROR] Fallada executant Nmap: " + e.getMessage());
            e.printStackTrace();
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
