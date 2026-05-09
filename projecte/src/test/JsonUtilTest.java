package test;

import java.util.Arrays;
import java.util.Locale;
import utils.JsonUtil;

public class JsonUtilTest {

    public static void run() {
        TestRunner.section("JsonUtil");

        TestRunner.test("escape: cometa i backslash", () -> {
            String s = JsonUtil.escape("a\"b\\c");
            TestRunner.assertEquals("a\\\"b\\\\c", s, "escape correcte");
        });

        TestRunner.test("escape: salts de línia i tabs", () -> {
            String s = JsonUtil.escape("a\nb\tc\rd");
            TestRunner.assertEquals("a\\nb\\tc\\rd", s, "control chars");
        });

        TestRunner.test("escape: caràcters de control < 0x20 com a \\uXXXX", () -> {
            String s = JsonUtil.escape("x");
            TestRunner.assertContains(s, "\\u0001", "unicode escape");
        });

        TestRunner.test("escape: null retorna cadena buida", () ->
            TestRunner.assertEquals("", JsonUtil.escape(null), "null safe"));

        TestRunner.test("formatDouble: SEMPRE punt decimal, mai coma", () -> {
            // Protecció contra futurs canvis de Locale.getDefault()
            Locale prev = Locale.getDefault();
            try {
                Locale.setDefault(new Locale("ca", "ES"));   // català: usa coma
                String s1 = JsonUtil.formatDouble(0.5);
                TestRunner.assertEquals("0.50", s1, "amb Locale ca_ES segueix sortint punt");

                Locale.setDefault(new Locale("es", "ES"));   // castellà: usa coma
                String s2 = JsonUtil.formatDouble(1.0 / 3.0);
                TestRunner.assertEquals("0.33", s2, "amb Locale es_ES segueix sortint punt");
            } finally {
                Locale.setDefault(prev);
            }
        });

        TestRunner.test("formatDouble: dos decimals consistents", () -> {
            TestRunner.assertEquals("1.00", JsonUtil.formatDouble(1.0),  "1");
            TestRunner.assertEquals("0.10", JsonUtil.formatDouble(0.10), "0.10");
        });

        TestRunner.test("intArray: format compacte", () -> {
            String s = JsonUtil.intArray(Arrays.asList(22, 80, 443));
            TestRunner.assertEquals("[22, 80, 443]", s, "format esperat");
        });

        TestRunner.test("intArray: buit", () ->
            TestRunner.assertEquals("[]", JsonUtil.intArray(null), "null → []"));
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
