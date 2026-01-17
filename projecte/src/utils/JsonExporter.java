package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import model.ResultatHost;

// Classe per exportar els resultats dels escanejos a format JSON
public class JsonExporter {
    
    // Metode principal exporta la llista de resultats a un fitxer JSON
    // Genera un array JSON amb tots els hosts escanejats
    // Parcialment fet amb IA
    public static boolean saveToJSON(List<ResultatHost> resultats, String path) {
        if (resultats == null || resultats.isEmpty()) {
            System.out.println(">>> [EXPORT] No hi ha resultats per exportar");
            return false; 
        }

        try (FileWriter writer = new FileWriter(path)) {
            writer.write("[\n");  

            for (int i = 0; i < resultats.size(); i++) {
                ResultatHost h = resultats.get(i);
                writer.write("  " + h.toJson());
                if (i < resultats.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("]");  
            
            System.out.println(">>> [EXPORT] JSON guardat a: " + path);
            return true;  
        
        } catch (IOException e) {
            System.err.println(">>> [ERROR] No s'ha pogut guardar el JSON: " + e.getMessage());
            return false;
        }
    }// Fi IA
    
    // Versio alternativa amb format Pretty
    // En comptes d'usar toJson(), construim el JSON manualment amb indentacio
    public static boolean saveToJSONPretty(List<ResultatHost> resultats, String path) {
        if (resultats == null || resultats.isEmpty()) {
            return false;
        }
        
        try (FileWriter writer = new FileWriter(path)) {
            writer.write("[\n");

            for (int i = 0; i < resultats.size(); i++) {
                ResultatHost h = resultats.get(i);
                writer.write("  {\n");
                writer.write("    \"ip\": \"" + h.getIp() + "\",\n");
                writer.write("    \"esViu\": " + h.isEsViu() + ",\n");  
                writer.write("    \"estat\": \"" + h.getEstat() + "\",\n");
                writer.write("    \"ports\": " + h.getPortsOberts() + "\n");  
                writer.write("  }");

                if (i < resultats.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("]\n");
            return true;
            
        } catch (IOException e) {
            return false; 
        }
    }
}