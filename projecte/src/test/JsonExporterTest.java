package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import model.ResultatHost;
import utils.JsonExporter;

public class JsonExporterTest {

    public static void run() {
        TestRunner.section("JsonExporter");

        TestRunner.test("saveToTopology: el risc s'imprimeix amb punt decimal", () -> {
            // Test crític demanat per l'usuari: el JSON exportat per RedTrace
            // ha de tenir 0.5 i no 0,5, fins i tot si el sistema està en català/castellà.
            Locale prev = Locale.getDefault();
            Path tmp = null;
            try {
                Locale.setDefault(new Locale("ca", "ES"));

                ResultatHost h1 = new ResultatHost("10.0.0.1");
                h1.setEsViu(true);
                h1.setPortsOberts(Arrays.asList(22, 80));

                ResultatHost h2 = new ResultatHost("10.0.0.2");
                h2.setEsViu(true);
                h2.setPortsOberts(Arrays.asList(445, 3389));

                tmp = Files.createTempFile("topo-", ".json");
                boolean ok = JsonExporter.saveToTopology(
                    Arrays.asList(h1, h2), tmp.toString());
                TestRunner.assertTrue(ok, "exportació correcta");

                String content = Files.readString(tmp);
                TestRunner.assertTrue(!content.contains("0,"),
                    "el JSON NO ha de contenir cap coma decimal — trobada en: " +
                    primeraLinaAmb(content, "0,"));
                TestRunner.assertContains(content, "\"risk\":", "té camp risk");
                TestRunner.assertContains(content, "\"weight\":", "té camp weight");
                TestRunner.assertContains(content, ".", "té punt decimal");
            } catch (IOException e) {
                throw new AssertionError("IO: " + e.getMessage());
            } finally {
                Locale.setDefault(prev);
                if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        });

        TestRunner.test("saveToTopology: rebutja llista buida", () -> {
            try {
                Path tmp = Files.createTempFile("topo-empty-", ".json");
                boolean ok = JsonExporter.saveToTopology(List.of(), tmp.toString());
                TestRunner.assertTrue(!ok, "ha de retornar false");
                Files.deleteIfExists(tmp);
            } catch (IOException e) {
                throw new AssertionError("IO: " + e.getMessage());
            }
        });

        TestRunner.test("saveToJSON: format simple amb hostname i risc", () -> {
            try {
                ResultatHost h = new ResultatHost("1.2.3.4");
                h.setEsViu(true);
                h.setHostname("router.local");
                h.setPortsOberts(Arrays.asList(80));

                Path tmp = Files.createTempFile("simple-", ".json");
                boolean ok = JsonExporter.saveToJSON(List.of(h), tmp.toString());
                TestRunner.assertTrue(ok, "exportació OK");

                String c = Files.readString(tmp);
                TestRunner.assertContains(c, "\"hostname\":\"router.local\"", "hostname");
                TestRunner.assertContains(c, "\"risc\":\"BAIX\"", "risc");
                Files.deleteIfExists(tmp);
            } catch (IOException e) {
                throw new AssertionError("IO: " + e.getMessage());
            }
        });
    }

    private static String primeraLinaAmb(String content, String needle) {
        for (String l : content.split("\n")) {
            if (l.contains(needle)) return l.trim();
        }
        return "(no trobada)";
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
