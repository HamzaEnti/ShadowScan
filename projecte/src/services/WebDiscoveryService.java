package services;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebDiscoveryService {

    private List<String> wordlist;
    private List<String> foundUrls;

    public WebDiscoveryService() {
        // Carreguem un diccionari basic per defecte
        this.wordlist = new ArrayList<>(Arrays.asList(
            "admin", "login", "test", "backup", "dashboard", 
            "config", "uploads", "images", "api", "wp-admin", "shell", "private"
        ));
        this.foundUrls = new ArrayList<>();
    }

    // Comprova si el servidor respon abans de començar l'atac massiu
    public boolean checkConnection(String baseUrl) {
        try {
            URL url = new URL(baseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void discoverWebPaths(String baseUrl) {
        System.out.println("Iniciant modul de reconeixement web...");

        // Normalitzacio de la URL
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://" + baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // Comprovacio de seguretat: El servidor es accessible?
        System.out.println("Verificant connectivitat amb " + baseUrl + "...");
        if (!checkConnection(baseUrl)) {
            System.err.println("ERROR: No s'ha pogut establir connexio amb l'objectiu.");
            System.err.println("Verifica que la URL es correcta o que el servidor esta actiu.");
            return; // Aturem l'execucio aqui
        }

        System.out.println("Objectiu actiu. Carregant diccionari (" + wordlist.size() + " entrades)...");
        this.foundUrls.clear();

        // Bucle de descobriment
        for (String word : wordlist) {
            String targetUrl = baseUrl + "/" + word;
            
            try {
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Usem GET amb timeout curt per agilitzar
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.connect();
                
                int status = conn.getResponseCode();

                // Filtrem el 404 (Not Found). Qualsevol altra cosa ens interessa.
                if (status != 404) {
                    String logEntry = " > [DIR] Descobert: /" + word + " (Codi: " + status + ")";
                    System.out.println(logEntry);
                    foundUrls.add(targetUrl + ";" + status);
                }

                conn.disconnect();

            } catch (Exception e) {
                // Silenciem errors de connexió puntuals per no embrutar la consola
            }
        }
        
        System.out.println("Escaneig finalitzat. Resultats totals: " + foundUrls.size());
    }

    // Metodes d'acces per a gestio de dades externa
    public List<String> getWordlist() {
        return wordlist;
    }

    public void setWordlist(List<String> wordlist) {
        this.wordlist = wordlist;
    }

    public List<String> getFoundUrls() {
        return foundUrls;
    }
}