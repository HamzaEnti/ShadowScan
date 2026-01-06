package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WebDiscoveryService {
  private List<String> foundUrls;
  private String customDictionaryPath;

  public WebDiscoveryService() {
    this.foundUrls = new ArrayList<>();
    this.customDictionaryPath = null;
  }

  // Comprova si dirb esta instalat
  public boolean checkToolInstalled() {
    try {
      ProcessBuilder pb = new ProcessBuilder("dirb", "-h");
      Process p = pb.start();
      int exitCode = p.waitFor();
      return exitCode == 0;
    } catch (Exception e) {
      return false;
    }
  }

  // Defineix el diccionari personalitzat
  public void setCustomDictionary(File dictionaryFile) {
    if (dictionaryFile != null && dictionaryFile.exists()) {
      this.customDictionaryPath = dictionaryFile.getAbsolutePath();
      System.out.println("Diccionari carregat: " + this.customDictionaryPath);
    } else {
      System.out.println("Error: El fitxer de diccionari no existeix.");
    }
  }

  // Executa l'atac
  public void discoverWebPaths(String baseUrl) {
    System.out.println("Iniciant escaneig extern amb DIRB sobre: " + baseUrl);
    this.foundUrls.clear();

    try {
      List<String> command = new ArrayList<>();
      command.add("dirb");
      command.add(baseUrl);

      if (customDictionaryPath != null) {
        command.add(customDictionaryPath);
      } else {
        System.out.println("Utilitzant diccionari per defecte.");
      }

      command.add("-r"); // No recursiu
      command.add("-S"); // Silent
      command.add("-N");
      command.add("404");

      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectErrorStream(true);

      Process process = pb.start();
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;

      while ((line = reader.readLine()) != null) {
        if (line.contains("CODE:200") || line.contains("DIRECTORY")
            || line.contains("+")) {
          System.out.println("DIRB " + line);

          // Netejem una mica la línia abans de guardar-la
          // Exemple entrada: "+ http://web.com/admin (CODE:200)"
          foundUrls.add(line);
        }
      }

      int exitCode = process.waitFor();
      System.out.println("Procés extern finalitzat amb codi: " + exitCode);

    } catch (Exception e) {
      System.err.println("Error executant dirb: " + e.getMessage());
    }
  }

  public boolean exportReportToCSV(File file) {
    if (foundUrls.isEmpty()) {
      System.out.println("No hi ha dades per exportar.");
      return false;
    }

    try (FileWriter writer = new FileWriter(file)) {
      // Escriure capçalera
      writer.write("RESULTAT_BRUT,DATA_ESCANEIG\n");

      // Escriure cada linia trobada
      for (String result : foundUrls) {
        // Afegim cometes per si el text te comes i evitem trencar el CSV
        writer.write(
            "\"" + result + "\",\"" + java.time.LocalDate.now() + "\"\n");
      }

      System.out.println(
          "Informe exportat correctament: " + file.getAbsolutePath());
      return true;

    } catch (IOException e) {
      System.err.println("Error: Escrivint el fitxer CSV: " + e.getMessage());
      return false;
    }
  }

  public List<String> getFoundUrls() {
    return foundUrls;
  }
}
