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

public class BruteForceService extends AbstractScanService {

    private String rutaUsuaris;
    private String rutaPasswords;
    // FIX: synchronized list i mètodes principals synchronized per evitar
    // que dos atacs concurrents s'entrecreuin la llista de resultats.
    private final List<String> resultats;

    public BruteForceService() {
        super("HYDRA");
        this.resultats = Collections.synchronizedList(new ArrayList<>());
    }

    // FIX: ara sí llegim el codi de sortida i tanquem el procés
    // Abans: new ProcessBuilder("hydra", "-h").start() → procés zombie, mai esperat
    @Override
    public boolean checkInstalled() {
        try {
            Process p = new ProcessBuilder("hydra", "-h").start();
            // hydra -h retorna codi 255 (ajuda), no 0 → qualsevol codi != excepció = instalat
            p.waitFor(5, TimeUnit.SECONDS);
            if (p.isAlive()) p.destroy();
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

    public synchronized void atacar(String ip, int port, String rutaUsers, String rutaPass) {
        if (!fitxerExisteix(rutaUsers)) {
            logError("Diccionari d'usuaris no trobat: " + rutaUsers);
            return;
        }
        if (!fitxerExisteix(rutaPass)) {
            logError("Diccionari de passwords no trobat: " + rutaPass);
            return;
        }

        String protocol = determinarProtocol(port);
        resultats.clear();

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
                resultats.add(line);
            }

            // FIX: timeout de 10 minuts, no esperem indefinidament
            boolean finished = p.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                p.destroy();
                logError("Hydra timeout (10 min) — procés aturat.");
            } else {
                log("Atac finalitzat.");
            }

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
