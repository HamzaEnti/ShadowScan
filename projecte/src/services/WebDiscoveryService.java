package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WebDiscoveryService {
    private List<String> foundUrls;
    private String customDictionaryPath;

    public WebDiscoveryService() {
        // Llista buida per guardar resultats
        this.foundUrls = new ArrayList<>();
        this.customDictionaryPath = null;
    }

    // Comprova si dirb està instal·lat
    public boolean checkInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("dirb", "-h");
            pb.start();
            return true;
        } catch (Exception e) {
            // Si falla, no està instal·lat
            return false;
        }
    }

    // Carrega diccionari personalitzat
    public void loadCustomDictionary(File dictionaryFile) {
        if (dictionaryFile != null && dictionaryFile.exists()) {
            this.customDictionaryPath = dictionaryFile.getAbsolutePath();
            System.out.println(">>> [DIRB] Diccionari seleccionat: " + dictionaryFile.getName());
        }
    }

    // Sobrecàrrega per passar IP i port
    public void discoverWebPaths(String targetIp, int port, String wordlistPath) {
        if (wordlistPath != null) this.customDictionaryPath = wordlistPath;
        String url = "http://" + targetIp + ":" + port + "/";
        discoverWebPaths(url);
    }

    // Escaneig principal
    public void discoverWebPaths(String baseUrl) {
        System.out.println(">>> [DIRB] Iniciant escaneig a: " + baseUrl);
        this.foundUrls.clear();

        try {
            List<String> command = new ArrayList<>();
            command.add("dirb");
            command.add(baseUrl);

            if (customDictionaryPath != null) {
                command.add(customDictionaryPath);
            }

            command.add("-r"); // No recursiu
            command.add("-S"); // Silent
            command.add("-N"); // Sense colors
            command.add("404"); // Ignora 404

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Junta stdout i stderr

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                // Mostra cada línia
                System.out.println("[DIRB] " + line);

                // Guarda només els èxits
                if (line.contains("CODE:200") || line.contains("DIRECTORY") || line.contains("+")) {
                    foundUrls.add(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println(">>> [DIRB] Finalitzat (Codi: " + exitCode + ")");

        } catch (Exception e) {
            // Error general
            System.err.println(">>> [ERROR] Dirb fallit: " + e.getMessage());
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
