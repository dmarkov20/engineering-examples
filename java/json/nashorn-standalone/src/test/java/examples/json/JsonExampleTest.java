package examples.json;

import org.junit.jupiter.api.Test;

import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonExampleTest {
    private static final String EXPECTED_SUMMARY =
            "OpenJDK 28 (active=true, language=Java, feature=JVM)";

    private final JsonExample example = new JsonExample();

    private static String fixture(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get("../fixtures", name)),
                StandardCharsets.UTF_8);
    }

    @Test
    void readsSample() throws Exception {
        assertEquals(EXPECTED_SUMMARY, JsonExample.describe(example.parse(fixture("sample.json"))));
    }

    @Test
    void readsEmptyContainers() throws Exception {
        assertTrue(((Map<?, ?>) example.parse(fixture("empty-object.json"))).isEmpty());
        assertTrue(((List<?>) example.parse(fixture("empty-array.json"))).isEmpty());
    }

    @Test
    void readsNestedValuesAndDistinguishesNullFromMissing() throws Exception {
        Map<?, ?> root = (Map<?, ?>) example.parse(fixture("edge-cases.json"));
        List<?> items = (List<?>) root.get("items");
        Map<?, ?> first = (Map<?, ?>) items.get(0);
        Map<?, ?> second = (Map<?, ?>) items.get(1);

        assertEquals("Quote: \" and newline:\nDublin ☘ Ω 🚀", first.get("text"));
        assertTrue(second.containsKey("value"));
        assertNull(second.get("value"));
        assertNull(second.get("missing"));
        assertFalse(second.containsKey("missing"));
    }

    @Test
    void roundsNumbersToBinary64Precision() throws Exception {
        Map<?, ?> root = (Map<?, ?>) example.parse(fixture("edge-cases.json"));
        assertEquals(-42, ((Number) root.get("integer")).intValue());
        assertEquals(0.12345678901234568, ((Number) root.get("decimal")).doubleValue());
        assertEquals(9007199254740992L, ((Number) root.get("largeInteger")).longValue());
        assertEquals(0, ((Number) root.get("decimal")).longValue());
        assertEquals(Long.MAX_VALUE, ((Number) root.get("outsideLong")).longValue());
    }

    @Test
    void keepsLastDuplicateValue() throws Exception {
        Map<?, ?> root = (Map<?, ?>) example.parse(fixture("duplicate-keys.json"));
        assertEquals(2, ((Number) root.get("value")).intValue());
    }

    @Test
    void rejectsTrailingTokens() throws Exception {
        String trailing = fixture("trailing-tokens.json");
        assertThrows(ScriptException.class, () -> example.parse(trailing));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        String malformed = fixture("malformed.json");
        assertThrows(ScriptException.class, () -> example.parse(malformed));
    }
}
