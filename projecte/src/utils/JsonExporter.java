package utils; 

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import model.ResultatHost;

public class JsonExporter {
  //exporta llista de ResultatHost a json
  public static boolean saveToJSON(List<ResultatHost> resultats, String path) {
    try (FileWriter writer = new FileWriter(path)) {
      writer.write("[\n"); // <-- inici array json

      for (int i = 0; i < resultats.size(); i++) {
        ResultatHost h = resultats.get(i);

        writer.write("  {");
        writer.write("\"ip\":\"" + h.getIp() + "\", ");
        writer.write("\"esViu\":" + h.isEsViu() + ", ");
        writer.write("\"ports\":" + h.getPortsOberts());
        writer.write("}");

        if (i < resultats.size() - 1) writer.write(","); // separa
        writer.write("\n");
      }

      writer.write("]"); // <-- fi array json
      return true;
    } catch (IOException e) {
      return false; 
    }
  }
}
