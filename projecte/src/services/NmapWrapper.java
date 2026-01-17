package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class NmapWrapper extends AbstractScanService {

    // llista on guardem les linies de sortida per poder exportar
    private List<String> resultats;

    public NmapWrapper() {
        super("NMAP");
        this.resultats = new ArrayList<>();
    }

    @Override
    public boolean checkInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("nmap", "--version");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return (exitCode == 0);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void executar(String ip, int port) {
        escanearConNmap(ip);
    }
    
    public void escanearConNmap(String ip) {
        log("Llancant comanda contra: " + ip);
        resultats.clear(); // netegem resultats anteriors

        try {
            ProcessBuilder pb = new ProcessBuilder("nmap", "-sV", "-T4", ip);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                resultats.add(line); // guardem cada linia
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log("Escaneig finalitzat amb exit.");
            } else {
                logError("Ha acabat amb errors (Codi: " + exitCode + ")");
            }

        } catch (Exception e) {
            logError("Fallada executant Nmap: " + e.getMessage());
            e.printStackTrace();
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