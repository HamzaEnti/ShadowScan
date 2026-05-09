package test;

import java.util.Arrays;
import java.util.List;
import model.EstatResultat;
import model.ResultatHost;
import model.RiskLevel;

public class ResultatHostTest {

    public static void run() {
        TestRunner.section("ResultatHost");

        TestRunner.test("Constructor: estat inicial OFFLINE/PENDENT", () -> {
            ResultatHost h = new ResultatHost("10.0.0.1");
            TestRunner.assertEquals("10.0.0.1", h.getIp(), "IP");
            TestRunner.assertTrue(!h.isEsViu(), "no està viu");
            TestRunner.assertTrue(h.getPortsOberts().isEmpty(), "sense ports");
        });

        TestRunner.test("setEsViu sincronitza l'estat heretat", () -> {
            ResultatHost h = new ResultatHost("10.0.0.1");
            h.setEsViu(true);
            TestRunner.assertEquals(EstatResultat.ONLINE, h.getEstat(), "ONLINE");
            h.setEsViu(false);
            TestRunner.assertEquals(EstatResultat.OFFLINE, h.getEstat(), "OFFLINE");
        });

        TestRunner.test("Ports: deduplicació via LinkedHashSet", () -> {
            ResultatHost h = new ResultatHost("10.0.0.1");
            h.afegirPort(80);
            h.afegirPort(80);
            h.afegirPort(443);
            h.afegirPort(80);
            List<Integer> ports = h.getPortsOberts();
            TestRunner.assertEquals(2, ports.size(), "només 2 ports únics");
            TestRunner.assertEquals(Integer.valueOf(80), ports.get(0), "ordre estable");
            TestRunner.assertEquals(Integer.valueOf(443), ports.get(1), "ordre estable");
        });

        TestRunner.test("setPortsOberts: deduplica una col·lecció amb duplicats", () -> {
            ResultatHost h = new ResultatHost("10.0.0.1");
            h.setPortsOberts(Arrays.asList(22, 80, 22, 443, 80));
            TestRunner.assertEquals(3, h.getPortsOberts().size(), "deduplicat a 3");
        });

        TestRunner.test("getRiskLevel: BAIX si no hi ha ports", () -> {
            ResultatHost h = new ResultatHost("10.0.0.1");
            TestRunner.assertEquals(RiskLevel.BAIX, h.getRiskLevel(), "BAIX");
        });

        TestRunner.test("getRiskLevel: CRITIC amb SMB", () -> {
            ResultatHost h = new ResultatHost("10.0.0.1");
            h.setPortsOberts(Arrays.asList(80, 445));
            TestRunner.assertEquals(RiskLevel.CRITIC, h.getRiskLevel(), "445 → CRITIC");
        });

        TestRunner.test("getRiskLevel: MITJA amb SSH", () -> {
            ResultatHost h = new ResultatHost("10.0.0.1");
            h.setPortsOberts(Arrays.asList(22, 80));
            TestRunner.assertEquals(RiskLevel.MITJA, h.getRiskLevel(), "22 → MITJA");
        });

        TestRunner.test("toJson: escapa cometes i conté tots els camps", () -> {
            ResultatHost h = new ResultatHost("1.2.3.4");
            h.setEsViu(true);
            h.setHostname("evil\"name");
            h.setPortsOberts(Arrays.asList(80, 443));
            String json = h.toJson();
            TestRunner.assertContains(json, "\"ip\":\"1.2.3.4\"", "ip");
            TestRunner.assertContains(json, "\"hostname\":\"evil\\\"name\"", "hostname escapat");
            TestRunner.assertContains(json, "\"esViu\":true", "esViu");
            TestRunner.assertContains(json, "\"risc\":\"BAIX\"", "risc");
            TestRunner.assertContains(json, "\"ports\":[80, 443]", "ports");
        });

        TestRunner.test("toJson: hostname null genera 'null' (no \"null\")", () -> {
            ResultatHost h = new ResultatHost("1.2.3.4");
            String json = h.toJson();
            TestRunner.assertContains(json, "\"hostname\":null", "null sense cometes");
        });
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
