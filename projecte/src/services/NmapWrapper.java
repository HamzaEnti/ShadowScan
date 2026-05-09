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

public class NmapWrapper extends AbstractScanService {

    // FIX: synchronized list per evitar curses si dos threads criden
    // escanearConNmap concurrentment (panel UI + verificació async).
    private final List<String> resultats;

    public NmapWrapper() {
        super("NMAP");
        this.resultats = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public boolean checkInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("nmap", "--version");
            Process p = pb.start();
            boolean done = p.waitFor(5, TimeUnit.SECONDS);
            if (!done) { p.destroy(); return false; }
            return (p.exitValue() == 0);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void executar(String ip, int port) {
        escanearConNmap(ip);
    }

    public synchronized void escanearConNmap(String ip) {
        log("Llancant comanda contra: " + ip);
        resultats.clear();

        try {
            ProcessBuilder pb = new ProcessBuilder("nmap", "-sV", "-T4", ip);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Bug fix: el BufferedReader no estava en try-with-resources.
            // Si readLine llançava IOException, el pipe del procés quedava
            // obert i la JVM acumulava descriptors fins esgotar-los.
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    resultats.add(line);
                }
            }

            // FIX: timeout de 5 minuts per evitar processos bloquejats indefinidament
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                logError("Nmap timeout (5 min) — procés aturat.");
            } else if (process.exitValue() == 0) {
                log("Escaneig finalitzat amb exit.");
            } else {
                logError("Ha acabat amb errors (Codi: " + process.exitValue() + ")");
            }

        } catch (Exception e) {
            logError("Fallada executant Nmap: " + e.getMessage());
        }
    }

    public synchronized List<String> getResultats() {
        return new ArrayList<>(resultats);
    }

    // FIX: LocalDateTime en comptes de new java.util.Date() (deprecated des de Java 8)
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
