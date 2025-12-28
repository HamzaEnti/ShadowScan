package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static List<String> llegirLinies(File fitxer) {
        List<String> linies = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fitxer))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isEmpty()) {
                    linies.add(linea);
                }
            }
        } catch (IOException e) {
            // Retornem llista buida en cas d'error
            return new ArrayList<>();
        }

        return linies;
    }
}