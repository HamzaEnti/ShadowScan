package utils; 

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
  //rep un objecte file i retorna una llista de línies
  public static List<String> llegirLinies(File fitxer) {
    List<String> linies = new ArrayList<>();
    //llista buida on guardarem les línies llegides

    try (BufferedReader br = new BufferedReader(new FileReader(fitxer))) {
      String linea;
      while ((linea = br.readLine()) != null) {
        linea = linea.trim();
        if (!linea.isEmpty()) {
          linies.add(linea);
          //afegim la línea no buida a la llista
        }
      }
    } catch (IOException e) {
      return new ArrayList<>();
    }
    return linies;
  }
}
