import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class JsonExampleTest {
    private static final String EXPECTED_SUMMARY =
            "OpenJDK 28 (active=true, language=Java, feature=JVM)";

    private final JsonExample example = new JsonExample();

    private static String fixture(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get("../fixtures", name)),
                StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (!JsonExampleTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Run tests with java -ea");
        }

        JsonExampleTest tests = new JsonExampleTest();
        tests.sample();
        tests.emptyContainers();
        tests.nestedValuesAndNull();
        tests.numbers();
        tests.keepsLastDuplicateValue();
        tests.assertParseError("malformed.json");
        tests.assertParseError("trailing-tokens.json");
        System.out.println("Nashorn JDK 8 checks passed");
    }

    private void sample() throws Exception {
        assert EXPECTED_SUMMARY.equals(JsonExample.describe(example.parse(fixture("sample.json"))));
    }

    private void emptyContainers() throws Exception {
        assert ((Map<?, ?>) example.parse(fixture("empty-object.json"))).isEmpty();
        assert ((List<?>) example.parse(fixture("empty-array.json"))).isEmpty();
    }

    private void nestedValuesAndNull() throws Exception {
        Map<?, ?> root = (Map<?, ?>) example.parse(fixture("edge-cases.json"));
        List<?> items = (List<?>) root.get("items");
        Map<?, ?> first = (Map<?, ?>) items.get(0);
        Map<?, ?> second = (Map<?, ?>) items.get(1);

        assert "Quote: \" and newline:\nDublin ☘ Ω 🚀".equals(first.get("text"));
        assert second.containsKey("value") && second.get("value") == null;
        assert second.get("missing") == null && !second.containsKey("missing");
    }

    private void numbers() throws Exception {
        Map<?, ?> root = (Map<?, ?>) example.parse(fixture("edge-cases.json"));
        assert ((Number) root.get("integer")).intValue() == -42;
        assert ((Number) root.get("decimal")).doubleValue() == 0.12345678901234568;
        assert ((Number) root.get("largeInteger")).longValue() == 9007199254740992L;
        assert ((Number) root.get("decimal")).longValue() == 0;
        assert ((Number) root.get("outsideLong")).longValue() == Long.MAX_VALUE;
    }

    private void keepsLastDuplicateValue() throws Exception {
        Map<?, ?> root = (Map<?, ?>) example.parse(fixture("duplicate-keys.json"));
        assert ((Number) root.get("value")).intValue() == 2;
    }

    private void assertParseError(String name) throws Exception {
        try {
            example.parse(fixture(name));
            throw new AssertionError("Expected ScriptException for " + name);
        } catch (ScriptException expected) {
            assert expected.getMessage().contains("SyntaxError") : expected;
        }
    }
}
