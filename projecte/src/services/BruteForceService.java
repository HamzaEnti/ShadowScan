package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class BruteForceService extends AbstractScanService {
    
    private String rutaUsuaris;
    private String rutaPasswords;
    private List<String> resultats;

    public BruteForceService() {
        super("HYDRA");
        this.resultats = new ArrayList<>();
    }

    @Override
    public boolean checkInstalled() {
        try {
            new ProcessBuilder("hydra", "-h").start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public void executar(String target, int port) {
        if (rutaUsuaris == null || rutaPasswords == null) {
            logError("Necessites carregar els diccionaris abans!");
            logError("Usa el metode atacar(ip, port, rutaUsers, rutaPass)");
            return;
        }
        atacar(target, port, rutaUsuaris, rutaPasswords);
    }
    
    public void setDiccionaris(String users, String pass) {
        this.rutaUsuaris = users;
        this.rutaPasswords = pass;
    }

    public void atacar(String ip, int port, String rutaUsers, String rutaPass) {
        if (!fitxerExisteix(rutaUsers)) {
            logError("Diccionari d'usuaris no trobat: " + rutaUsers);
            return;
        }
        if (!fitxerExisteix(rutaPass)) {
            logError("Diccionari de passwords no trobat: " + rutaPass);
            return;
        }

        String protocol = determinarProtocol(port);
        resultats.clear(); // netegem resultats anteriors

        log("Iniciant atac a " + ip + " pel port " + port + " (" + protocol + ")...");
        log("Diccionaris carregats. Aixo pot tardar una estona.");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "hydra",
                "-L", rutaUsers,
                "-P", rutaPass,
                "-s", String.valueOf(port),
                "-t", "4",
                "-I",
                protocol + "://" + ip
            );

            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream())
            );
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println("[HYDRA] " + line);
                resultats.add(line); // guardem cada linia
            }

            p.waitFor();
            log("Atac finalitzat.");

        } catch (Exception e) {
            logError("Hydra error: " + e.getMessage());
        }
    }
    
    private String determinarProtocol(int port) {
        switch (port) {
            case 21:   return "ftp";
            case 22:   return "ssh";
            case 23:   return "telnet";
            case 80:   return "http-get";
            case 443:  return "https-get";
            case 3306: return "mysql";
            case 3389: return "rdp";
            case 5432: return "postgres";
            default:   return "ssh";
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