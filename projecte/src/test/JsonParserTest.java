package test;

import java.util.List;
import java.util.Map;
import utils.JsonParser;

public class JsonParserTest {

    public static void run() {
        TestRunner.section("JsonParser");

        TestRunner.test("Objecte simple", () -> {
            Map<String, Object> m = JsonParser.parseObject("{\"a\":1,\"b\":\"x\"}");
            TestRunner.assertEquals(1L, m.get("a"), "a");
            TestRunner.assertEquals("x", m.get("b"), "b");
        });

        TestRunner.test("Array de números", () -> {
            List<Object> a = JsonParser.parseArray("[1, 2.5, -3]");
            TestRunner.assertEquals(3, a.size(), "mida");
            TestRunner.assertEquals(1L, a.get(0), "long");
            TestRunner.assertEquals(2.5, a.get(1), "double");
        });

        TestRunner.test("Niu profund + path accessor", () -> {
            String s = "{\"data\":{\"items\":[{\"id\":\"X1\",\"score\":7.5}]}}";
            Object root = JsonParser.parse(s);
            TestRunner.assertEquals("X1", JsonParser.getString(root, "data.items.0.id"), "id");
            TestRunner.assertEquals(7.5, JsonParser.getDouble(root, "data.items.0.score"), "score");
        });

        TestRunner.test("Escapes a strings", () -> {
            Map<String, Object> m = JsonParser.parseObject("{\"s\":\"a\\\"b\\nc\\tD\"}");
            TestRunner.assertEquals("a\"b\nc\tD", m.get("s"), "escapes");
        });

        TestRunner.test("Unicode escape \\u00e9", () -> {
            Map<String, Object> m = JsonParser.parseObject("{\"x\":\"caf\\u00e9\"}");
            TestRunner.assertEquals("café", m.get("x"), "unicode");
        });

        TestRunner.test("Booleans, null", () -> {
            Map<String, Object> m = JsonParser.parseObject("{\"t\":true,\"f\":false,\"n\":null}");
            TestRunner.assertEquals(Boolean.TRUE,  m.get("t"), "true");
            TestRunner.assertEquals(Boolean.FALSE, m.get("f"), "false");
            TestRunner.assertTrue(m.containsKey("n") && m.get("n") == null, "null");
        });

        TestRunner.test("get amb path inexistent retorna null", () -> {
            Object root = JsonParser.parse("{\"a\":{\"b\":1}}");
            TestRunner.assertEquals(null, JsonParser.get(root, "a.c.d"), "null safe");
        });
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
