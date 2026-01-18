package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class WebDiscoveryService extends AbstractScanService {
    
    private List<String> foundUrls;
    private String customDictionaryPath;

    public WebDiscoveryService() {
        super("DIRB");
        this.foundUrls = new ArrayList<>();
        this.customDictionaryPath = null;
    }

    // comprova si dirb esta instalat
    @Override
    public boolean checkInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("dirb", "-h");
            pb.start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // implementacio de la interficie
    // construim la URL i cridem al metode principal
    @Override
    public void executar(String target, int port) {
        String url = "http://" + target + ":" + port + "/";
        discoverWebPaths(url);
    }

    // carrega un diccionari personalitzat
    public void loadCustomDictionary(File dictionaryFile) {
        if (dictionaryFile != null && dictionaryFile.exists()) {
            this.customDictionaryPath = dictionaryFile.getAbsolutePath();
            log("Diccionari seleccionat: " + dictionaryFile.getName());
        }
    }

    // sobrecarrega per passar IP i port directament
    public void discoverWebPaths(String targetIp, int port, String wordlistPath) {
        if (wordlistPath != null) {
            this.customDictionaryPath = wordlistPath;
        }
        String url = "http://" + targetIp + ":" + port + "/";
        discoverWebPaths(url);
    }

    // metode principal d'escaneig
    public void discoverWebPaths(String baseUrl) {
        log("Iniciant escaneig a: " + baseUrl);
        this.foundUrls.clear();

        try {
            // construim la comanda
            List<String> command = new ArrayList<>();
            command.add("dirb");
            command.add(baseUrl);

            // si tenim diccionari custom, l'afegim
            if (customDictionaryPath != null) {
                command.add(customDictionaryPath);
            }

            // opcions de dirb:
            // -r = no recursiu (mes rapid)
            // -S = mode silencis (menys soroll)
            // -N 404 = ignora respostes 404
            command.add("-r");
            command.add("-S");
            command.add("-N");
            command.add("404");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println("[DIRB] " + line);

                // guardem nomes els exits (codi 200, directoris, etc)
                if (line.contains("CODE:200") || 
                    line.contains("DIRECTORY") || 
                    line.contains("+")) {
                    foundUrls.add(line);
                }
            }

            int exitCode = process.waitFor();
            log("Finalitzat (Codi: " + exitCode + ")");

        } catch (Exception e) {
            logError("Dirb fallit: " + e.getMessage());
        }
    }
    
    // retorna la llista de URLs trobades
    public List<String> getFoundUrls() {
        return new ArrayList<>(foundUrls); // retornem copia per seguretat
    }

    // exporta resultats a CSV
    public boolean exportReportToCSV(File file) {
        if (foundUrls.isEmpty()) {
            log("No hi ha resultats per exportar.");
            return false;
        }
        
        try (FileWriter writer = new FileWriter(file)) {
            // capcalera del CSV
            writer.write("URL,DATA\n");
            
            // cada resultat en una linia
            for (String s : foundUrls) {
                // escapem les cometes per si de cas
                String escaped = s.replace("\"", "\"\"");
                writer.write("\"" + escaped + "\",\"" + new java.util.Date() + "\"\n");
            }
            
            log("CSV Exportat: " + file.getAbsolutePath());
            return true;
            
        } catch (IOException e) {
            logError("Error exportant CSV: " + e.getMessage());
            return false;
        }
    }
}
