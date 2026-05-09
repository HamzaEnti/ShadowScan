package test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Mini-framework d'asserts per a tests sense dependència de JUnit.
 *
 * Permet executar tots els tests del projecte amb un sol main() i sense
 * configurar build system. Cada classe de test crida {@code TestRunner.test(...)}
 * i finalment {@code TestRunner.summary()} per imprimir el resultat.
 */
public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void test(String name, Runnable r) {
        try {
            r.run();
            passed++;
            System.out.println("  [PASS] " + name);
        } catch (AssertionError | Exception e) {
            failed++;
            failures.add(name + " — " + e.getMessage());
            System.out.println("  [FAIL] " + name + " — " + e.getMessage());
        }
    }

    public static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    public static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    public static void assertEquals(Object expected, Object actual, String msg) {
        boolean eq = (expected == null) ? actual == null : expected.equals(actual);
        if (!eq) {
            throw new AssertionError(msg + " — esperat='" + expected + "', actual='" + actual + "'");
        }
    }

    public static void assertContains(String haystack, String needle, String msg) {
        if (haystack == null || !haystack.contains(needle)) {
            throw new AssertionError(msg + " — la cadena no conté '" + needle + "': " + haystack);
        }
    }

    public static <T> T expectThrows(Class<? extends Throwable> ex, Supplier<T> s) {
        try {
            s.get();
        } catch (Throwable t) {
            if (ex.isInstance(t)) return null;
            throw new AssertionError("Excepció inesperada: " + t.getClass().getSimpleName());
        }
        throw new AssertionError("S'esperava " + ex.getSimpleName() + " però no es va llençar res");
    }

    public static int summary() {
        System.out.println("\n────────────────────────────────────────");
        System.out.println("Total: " + (passed + failed) + " | Passats: " + passed + " | Fallits: " + failed);
        if (!failures.isEmpty()) {
            System.out.println("Fallits:");
            for (String f : failures) System.out.println("  - " + f);
        }
        System.out.println("────────────────────────────────────────");
        return failed;
    }

    public static void main(String[] args) {
        NetworkUtilTest.run();
        ResultatHostTest.run();
        RiskLevelTest.run();
        JsonUtilTest.run();
        JsonExporterTest.run();
        int failed = summary();
        System.exit(failed == 0 ? 0 : 1);
    }
}
