package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import model.ResultatHost;

public class JsonExporter {

    public static boolean saveToJSON(List<ResultatHost> resultats, String path) {
        try (FileWriter writer = new FileWriter(path)) {

            writer.write("[\n");

            for (int i = 0; i < resultats.size(); i++) {
                ResultatHost h = resultats.get(i);

                writer.write("  {");
                writer.write("\"ip\":\"" + h.getIp() + "\", ");
                writer.write("\"esViu\":" + h.isEsViu() + ", ");
                writer.write("\"ports\":" + h.getPortsOberts());
                writer.write("}");

                if (i < resultats.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("]");
            return true;

        } catch (IOException e) {
            return false;
        }
    }
}
