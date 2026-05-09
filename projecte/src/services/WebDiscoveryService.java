package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WebDiscoveryService extends AbstractScanService {

    // FIX: synchronized list i mètodes principals synchronized.
    private final List<String> foundUrls;
    private String customDictionaryPath;

    public WebDiscoveryService() {
        super("DIRB");
        this.foundUrls = Collections.synchronizedList(new ArrayList<>());
        this.customDictionaryPath = null;
    }

    // FIX: tancam el procés, igual que WebDiscoveryService tenia el mateix problema de zombie que Hydra
    @Override
    public boolean checkInstalled() {
        try {
            Process p = new ProcessBuilder("dirb").start();
            p.waitFor(5, TimeUnit.SECONDS);
            if (p.isAlive()) p.destroy();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void executar(String target, int port) {
        String url = "http://" + target + ":" + port + "/";
        discoverWebPaths(url);
    }

    public void loadCustomDictionary(File dictionaryFile) {
        if (dictionaryFile != null && dictionaryFile.exists()) {
            this.customDictionaryPath = dictionaryFile.getAbsolutePath();
            log("Diccionari seleccionat: " + dictionaryFile.getName());
        }
    }

    public void discoverWebPaths(String targetIp, int port, String wordlistPath) {
        if (wordlistPath != null) {
            this.customDictionaryPath = wordlistPath;
        }
        String url = "http://" + targetIp + ":" + port + "/";
        discoverWebPaths(url);
    }

    public synchronized void discoverWebPaths(String baseUrl) {
        log("Iniciant escaneig a: " + baseUrl);
        this.foundUrls.clear();

        try {
            List<String> command = new ArrayList<>();
            command.add("dirb");
            command.add(baseUrl);

            if (customDictionaryPath != null) {
                command.add(customDictionaryPath);
            }

            command.add("-r");
            command.add("-S");
            command.add("-N");
            command.add("404");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            // Bug fix: try-with-resources per evitar leak de descriptors.
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[DIRB] " + line);
                    if (line.contains("CODE:200") ||
                        line.contains("DIRECTORY") ||
                        line.contains("+")) {
                        foundUrls.add(line);
                    }
                }
            }

            // FIX: timeout de 10 minuts
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                logError("Dirb timeout (10 min) — procés aturat.");
            } else {
                log("Finalitzat (Codi: " + process.exitValue() + ")");
            }

        } catch (Exception e) {
            logError("Dirb fallit: " + e.getMessage());
        }
    }

    public synchronized List<String> getFoundUrls() {
        return new ArrayList<>(foundUrls);
    }

    // FIX: LocalDateTime en comptes de java.util.Date (deprecated)
    public synchronized boolean exportReportToCSV(File file) {
        if (foundUrls.isEmpty()) {
            log("No hi ha resultats per exportar.");
            return false;
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("URL,TIMESTAMP\n");
            String timestamp = LocalDateTime.now().toString();

            for (String s : foundUrls) {
                String escaped = s.replace("\"", "\"\"");
                writer.write("\"" + escaped + "\",\"" + timestamp + "\"\n");
            }

            log("CSV Exportat: " + file.getAbsolutePath());
            return true;

        } catch (IOException e) {
            logError("Error exportant CSV: " + e.getMessage());
            return false;
        }
    }
}
