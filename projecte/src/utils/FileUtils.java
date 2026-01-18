package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Classe d'utilitats per treballar amb fitxers
public class FileUtils {
    
    //Parcialment fet amb IA
    public static List<String> llegirLinies(File fitxer) {
        List<String> linies = new ArrayList<>();

     
        if (fitxer == null || !fitxer.exists()) {
            System.err.println(">>> [ERROR] Fitxer no existeix: " + fitxer);
            return linies;  
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fitxer))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();  
                if (!linea.isEmpty()) {
                    linies.add(linea);
                }
            }
        } catch (IOException e) {
            System.err.println(">>> [ERROR] Error llegint fitxer: " + e.getMessage());
            return new ArrayList<>();  
        }
        
        return linies;
    }// Fi IA
    
    // Sobrecarrega del metode anterior per acceptar un String en comptes d'un File
    public static List<String> llegirLinies(String path) {
        return llegirLinies(new File(path));
    }
    
    // Metode per comprovar si un fitxer existeix i es pot llegir
    public static boolean existeixILlegible(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        File f = new File(path);
        return f.exists() && f.isFile() && f.canRead();
    }
}