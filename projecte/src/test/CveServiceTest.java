package test;

import java.util.List;
import model.CveEntry;
import services.CveService;

public class CveServiceTest {

    public static void run() {
        TestRunner.section("CveService (offline mode)");

        CveService svc = new CveService();
        svc.setOfflineMode(true);

        TestRunner.test("openssh té entrades al catàleg local", () -> {
            List<CveEntry> r = svc.lookup("openssh", null);
            TestRunner.assertTrue(!r.isEmpty(), "no buit");
            TestRunner.assertTrue(r.get(0).getCvssScore() >= r.get(r.size() - 1).getCvssScore(),
                "ordenat per CVSS desc");
        });

        TestRunner.test("smb retorna EternalBlue/SMBGhost", () -> {
            List<CveEntry> r = svc.lookup("smb", null);
            boolean trobaCritic = false;
            for (CveEntry e : r) {
                if (e.getId().contains("2017-0144") || e.getId().contains("2020-0796")) {
                    trobaCritic = true;
                    break;
                }
            }
            TestRunner.assertTrue(trobaCritic, "ha de contenir EternalBlue o SMBGhost");
        });

        TestRunner.test("Servei desconegut retorna llista buida", () -> {
            List<CveEntry> r = svc.lookup("xyzzy-not-a-service", null);
            TestRunner.assertTrue(r.isEmpty(), "buit");
        });

        TestRunner.test("Cache: segona crida retorna mateix resultat", () -> {
            List<CveEntry> a = svc.lookup("apache", null);
            List<CveEntry> b = svc.lookup("apache", null);
            TestRunner.assertEquals(a.size(), b.size(), "mida coincideix");
        });

        TestRunner.test("CveEntry.toJson amb decimal punt (Locale-safe)", () -> {
            java.util.Locale prev = java.util.Locale.getDefault();
            try {
                java.util.Locale.setDefault(new java.util.Locale("ca", "ES"));
                CveEntry e = new CveEntry("CVE-X", "desc", 7.5, "HIGH", "u");
                String json = e.toJson();
                TestRunner.assertContains(json, "\"cvss\":7.50", "punt decimal");
                TestRunner.assertTrue(!json.contains("7,5"), "no coma");
            } finally {
                java.util.Locale.setDefault(prev);
            }
        });
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
