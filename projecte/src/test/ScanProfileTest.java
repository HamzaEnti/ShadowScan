package test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import model.ScanProfile;
import utils.ProfileStore;

public class ScanProfileTest {

    public static void run() {
        TestRunner.section("ScanProfile + ProfileStore");

        TestRunner.test("toJson/fromJson round-trip", () -> {
            ScanProfile p = new ScanProfile("DMZ", "10.0.0.1", "10.0.0.50", "PARCIAL");
            p.setUdp(true);
            p.setCustomPorts(Arrays.asList(22, 80, 443));
            String json = p.toJson();
            ScanProfile q = ScanProfile.fromJson(json);
            TestRunner.assertEquals("DMZ", q.getName(), "name");
            TestRunner.assertEquals("10.0.0.1", q.getStartIp(), "start");
            TestRunner.assertEquals("10.0.0.50", q.getEndIp(), "end");
            TestRunner.assertEquals("PARCIAL", q.getMode(), "mode");
            TestRunner.assertTrue(q.isUdp(), "udp");
            TestRunner.assertEquals(Integer.valueOf(443), q.getCustomPorts().get(2), "ports");
        });

        TestRunner.test("ProfileStore: save + loadAll", () -> {
            try {
                Path tmp = Files.createTempDirectory("shadowscan-profiles-");
                ProfileStore store = new ProfileStore(tmp);

                ScanProfile a = new ScanProfile("Inventari", "192.168.0.1", "192.168.0.254", "PARCIAL");
                ScanProfile b = new ScanProfile("Audit DMZ", "10.0.0.1", "10.0.0.50", "FULL");
                store.save(a);
                store.save(b);

                List<ScanProfile> all = store.loadAll();
                TestRunner.assertEquals(2, all.size(), "2 perfils");
                // Ordre alfabètic: "Audit DMZ" abans de "Inventari"
                TestRunner.assertEquals("Audit DMZ", all.get(0).getName(), "ordre");

                TestRunner.assertTrue(store.delete("Audit DMZ"), "delete OK");
                TestRunner.assertEquals(1, store.loadAll().size(), "1 després de borrar");

                // Neteja
                for (Path p : Files.list(tmp).toList()) Files.deleteIfExists(p);
                Files.deleteIfExists(tmp);
            } catch (Exception e) {
                throw new AssertionError(e.getMessage());
            }
        });

        TestRunner.test("Decimal fix-protected: nom amb cometes s'escapa", () -> {
            ScanProfile p = new ScanProfile("a\"b", "1.1.1.1", "1.1.1.2", "PARCIAL");
            String json = p.toJson();
            TestRunner.assertContains(json, "\"name\":\"a\\\"b\"", "escape correcte");
        });
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
