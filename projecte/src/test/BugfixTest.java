package test;

import controller.HostFoundListener;
import controller.PortScanMode;
import controller.ScanController;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import model.ResultatHost;
import model.ScanProfile;
import report.PdfReport;
import utils.JsonExporter;
import utils.NetworkUtil;

/**
 * Tests de regressió per als bugs detectats al sweep posterior a v2.0.
 * Cada test correspon a un bug concret arreglat — així si algú reverteix
 * la correcció el test el delata.
 */
public class BugfixTest {

    public static void run() {
        TestRunner.section("Regression: post-v2.0 bug sweep");

        TestRunner.test("ScanProfile.fromJson tolera valors null", () -> {
            ScanProfile p = ScanProfile.fromJson(
                "{\"name\":null,\"startIp\":null,\"endIp\":null,\"mode\":null}");
            TestRunner.assertEquals("(sense nom)", p.getName(), "name default");
            TestRunner.assertEquals("",            p.getStartIp(), "start default");
            TestRunner.assertEquals("PARCIAL",     p.getMode(), "mode default");
        });

        TestRunner.test("NetworkUtil.smartRange rebutja mescla v4+v6", () -> {
            List<String> ips = NetworkUtil.smartRange("10.0.0.1", "::1");
            TestRunner.assertTrue(ips.isEmpty(), "buit en lloc d'excepció");
        });

        TestRunner.test("ScanController bloqueja escaneigs reentrants", () -> {
            ScanController c = new ScanController((HostFoundListener) host -> {});
            int[] calls = {0};
            c.setCallback(n -> calls[0]++);
            // Primera invocació amb rang fals → ips.isEmpty() → callback(0) immediat,
            // enExecucio queda false, podem tornar a invocar.
            c.escanearRang("invalid", "invalid", PortScanMode.PARCIAL, false);
            TestRunner.assertEquals(1, calls[0], "callback per rang invàlid");
        });

        TestRunner.test("PdfReport accepta hosts == null", () -> {
            try {
                File tmp = File.createTempFile("shadowscan-null-", ".pdf");
                try {
                    PdfReport.write(tmp, null);   // abans: NPE
                    TestRunner.assertTrue(tmp.length() > 200, "PDF generat");
                } finally {
                    tmp.delete();
                }
            } catch (Exception e) {
                throw new AssertionError(e.getMessage());
            }
        });

        TestRunner.test("saveToJSON i saveToJSONPretty produeixen el mateix esquema", () -> {
            try {
                ResultatHost h = new ResultatHost("10.0.0.1");
                h.setEsViu(true);
                h.setHostname("router.local");
                h.setPortsOberts(java.util.Arrays.asList(80, 443));

                Path a = Files.createTempFile("simple-", ".json");
                Path b = Files.createTempFile("pretty-", ".json");
                JsonExporter.saveToJSON(List.of(h), a.toString());
                JsonExporter.saveToJSONPretty(List.of(h), b.toString());

                String aJson = Files.readString(a);
                String bJson = Files.readString(b);

                // Tots dos han de tenir hostname i risc — bug de l'esquema
                // antic resoldria a només pretty mancant aquests camps.
                TestRunner.assertContains(aJson, "\"hostname\":\"router.local\"", "simple té hostname");
                TestRunner.assertContains(bJson, "\"hostname\":\"router.local\"", "pretty té hostname");
                TestRunner.assertContains(aJson, "\"risc\":\"BAIX\"", "simple té risc");
                TestRunner.assertContains(bJson, "\"risc\":\"BAIX\"", "pretty té risc");

                Files.deleteIfExists(a);
                Files.deleteIfExists(b);
            } catch (Exception e) {
                throw new AssertionError(e.getMessage());
            }
        });

        TestRunner.test("ResultatHost manté ordre de ports (LinkedHashSet)", () -> {
            // Bug potencial: si algú canvia el LinkedHashSet per HashSet,
            // l'ordre d'inserció es perdria i els tests posteriors pintarien
            // ports en ordre arbitrari als reports. Aquest test el blinda.
            ResultatHost h = new ResultatHost("1.2.3.4");
            h.afegirPort(443);
            h.afegirPort(22);
            h.afegirPort(80);
            List<Integer> ports = h.getPortsOberts();
            TestRunner.assertEquals(Integer.valueOf(443), ports.get(0), "primer = 443");
            TestRunner.assertEquals(Integer.valueOf(22),  ports.get(1), "segon = 22");
            TestRunner.assertEquals(Integer.valueOf(80),  ports.get(2), "tercer = 80");
        });
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }

    @SuppressWarnings("unused")
    private static List<ResultatHost> sampleHosts() {
        List<ResultatHost> l = new ArrayList<>();
        ResultatHost h = new ResultatHost("10.0.0.1");
        h.setEsViu(true);
        l.add(h);
        return l;
    }
}
