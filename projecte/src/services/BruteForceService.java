package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BruteForceService {
    private List<String> foundUrls;
    public BruteForceService() {
        // Llista buida per guardar resultats
        this.foundUrls = new ArrayList<>();
    }

    public boolean checkInstalled() {
        try {
            // Prova si hydra està instal·lat
            new ProcessBuilder("hydra", "-h").start();
            return true;
        } catch (Exception e) {
            // Si peta, no està instal·lat
            return false;
        }
    }

    public void atacar(String ip, int port, String rutaUsers, String rutaPass) {
        // Comprova que els diccionaris existeixen
        if (!new File(rutaUsers).exists() || !new File(rutaPass).exists()) {
            System.err.println(">>> [ERROR] Els fitxers de diccionari no existeixen.");
            return;
        }

        // Protocol per defecte ssh, canvia segons port
        String protocol = "ssh"; 
        if (port == 21) protocol = "ftp";
        if (port == 80) protocol = "http-get";
        if (port == 3306) protocol = "mysql";

        // Missatges d’inici
        System.out.println(">>> [HYDRA] Iniciant atac a " + ip + " pel port " + port + " (" + protocol + ")...");
        System.out.println(">>> [HYDRA] Diccionaris carregats. Això pot tardar.");

        try {
            // Configura la comanda hydra
            ProcessBuilder pb = new ProcessBuilder(
                "hydra",
                "-L", rutaUsers,
                "-P", rutaPass,
                "-s", String.valueOf(port),
                "-t", "4", // 4 fils
                "-I",      // Ignora restore file
                protocol + "://" + ip
            );

            pb.redirectErrorStream(true); // Hydra escriu molt per stderr
            Process p = pb.start();

            // Llegeix la sortida en temps real
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                // Mostra cada línia per veure el progrés
                System.out.println("[HYDRA] " + line);
            }

            // Espera que acabi
            p.waitFor();
            System.out.println(">>> [HYDRA] Atac finalitzat.");

        } catch (Exception e) {
            // Error general
            System.err.println(">>> [ERROR] Hydra error: " + e.getMessage());
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