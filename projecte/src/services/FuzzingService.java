package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class FuzzingService extends AbstractScanService {
    
    private String wordlistPath;
    private List<String> resultats;

    public FuzzingService() {
        super("FFUF");
        this.resultats = new ArrayList<>();
    }

    @Override
    public boolean checkInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffuf", "-version");
            Process p = pb.start();
            return (p.waitFor() == 0);
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

    public void lanzarFuzzing(String ip, int port, String rutaWordlist) {
        if (!fitxerExisteix(rutaWordlist)) {
            logError("Wordlist no trobada: " + rutaWordlist);
            return;
        }

        String url = "http://" + ip + ":" + port + "/FUZZ";
        log("Fuzzing a: " + url);
        resultats.clear(); // netegem resultats anteriors

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffuf", 
                "-w", rutaWordlist, 
                "-u", url, 
                "-mc", "200,301,302,403"
            );

            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream())
            );
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println("[FFUF] " + line);
                resultats.add(line); // guardem cada linia
            }

            p.waitFor();
            log("Finalitzat.");

        } catch (Exception e) {
            logError("Ffuf error: " + e.getMessage());
        }
    }
    
    // retorna els resultats guardats
    public List<String> getResultats() {
        return new ArrayList<>(resultats);
    }
    
    // exporta resultats a CSV
    public boolean exportToCSV(File file) {
        if (resultats.isEmpty()) {
            log("No hi ha resultats per exportar.");
            return false;
        }
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("LINIA,CONTINGUT,DATA\n");
            
            int numLinia = 1;
            for (String s : resultats) {
                String escaped = s.replace("\"", "\"\"");
                writer.write(numLinia + ",\"" + escaped + "\",\"" + new java.util.Date() + "\"\n");
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