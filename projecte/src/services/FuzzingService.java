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

public class FuzzingService extends AbstractScanService {

    private String wordlistPath;
    // FIX: synchronized list per a accessos concurrents.
    private final List<String> resultats;

    public FuzzingService() {
        super("FFUF");
        this.resultats = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public boolean checkInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffuf", "-version");
            Process p = pb.start();
            boolean done = p.waitFor(5, TimeUnit.SECONDS);
            if (!done) { p.destroy(); return false; }
            return true; // ffuf retorna != 0 per -version però no llença excepció si existeix
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void executar(String target, int port) {
        if (wordlistPath == null) {
            logError("Necessites configurar la wordlist primer!");
            return;
        }
        lanzarFuzzing(target, port, wordlistPath);
    }

    public void setWordlist(String path) {
        this.wordlistPath = path;
    }

    public synchronized void lanzarFuzzing(String ip, int port, String rutaWordlist) {
        if (!fitxerExisteix(rutaWordlist)) {
            logError("Wordlist no trobada: " + rutaWordlist);
            return;
        }

        String url = "http://" + ip + ":" + port + "/FUZZ";
        log("Fuzzing a: " + url);
        resultats.clear();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffuf",
                "-w", rutaWordlist,
                "-u", url,
                "-mc", "200,301,302,403"
            );

            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Bug fix: try-with-resources per evitar leak de descriptors.
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[FFUF] " + line);
                    resultats.add(line);
                }
            }

            // FIX: timeout de 10 minuts
            boolean finished = p.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                p.destroyForcibly();
                logError("Ffuf timeout (10 min) — procés aturat.");
            } else {
                log("Finalitzat.");
            }

        } catch (Exception e) {
            logError("Ffuf error: " + e.getMessage());
        }
    }

    public synchronized List<String> getResultats() {
        return new ArrayList<>(resultats);
    }

    // FIX: LocalDateTime en comptes de java.util.Date (deprecated)
    public synchronized boolean exportToCSV(File file) {
        if (resultats.isEmpty()) {
            log("No hi ha resultats per exportar.");
            return false;
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("LINIA,CONTINGUT,TIMESTAMP\n");
            String timestamp = LocalDateTime.now().toString();

            int numLinia = 1;
            for (String s : resultats) {
                String escaped = s.replace("\"", "\"\"");
                writer.write(numLinia + ",\"" + escaped + "\",\"" + timestamp + "\"\n");
                numLinia++;
            }

            log("CSV Exportat: " + file.getAbsolutePath());
            return true;

        } catch (IOException e) {
            logError("Error exportant CSV: " + e.getMessage());
            return false;
        }
    }
}
