package examples.json;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonExampleTest {
    private static String fixture(String name) throws IOException {
        return Files.readString(Path.of("../fixtures", name));
    }

    @Test
    void readsSample() throws Exception {
        assertEquals("OpenJDK 28 (active=true, language=Java, feature=JVM)",
                JsonExample.describe(JsonExample.parse(fixture("sample.json"))));
    }

    @Test
    void readsEmptyContainers() throws Exception {
        JsonNode object = JsonExample.parse(fixture("empty-object.json"));
        JsonNode array = JsonExample.parse(fixture("empty-array.json"));
        assertTrue(object.isObject() && object.isEmpty());
        assertTrue(array.isArray() && array.isEmpty());
    }

    @Test
    void readsNestedValuesAndDistinguishesNullFromMissing() throws Exception {
        JsonNode items = JsonExample.parse(fixture("edge-cases.json")).get("items");
        assertEquals("Quote: \" and newline:\nDublin ☘ Ω 🚀", items.get(0).get("text").textValue());
        assertTrue(items.get(1).get("value").isNull());
        assertNull(items.get(1).get("missing"));
        assertTrue(items.get(1).path("missing").isMissingNode());
        assertThrows(IllegalArgumentException.class, () -> items.get(1).required("missing"));
    }

    @Test
    void preservesIntegersAndConfiguredDecimalPrecision() throws Exception {
        JsonNode root = JsonExample.parse(fixture("edge-cases.json"));
        assertEquals(-42, root.get("integer").intValue());
        JsonNode decimal = root.get("decimal");
        assertEquals(new BigDecimal("0.1234567890123456789"), decimal.decimalValue());
        assertEquals(9007199254740993L, root.get("largeInteger").longValue());

        assertEquals(0, decimal.longValue());
        assertFalse(decimal.canConvertToExactIntegral());
        JsonNode outsideLong = root.get("outsideLong");
        assertEquals(new BigInteger("9223372036854775808"), outsideLong.bigIntegerValue());
        assertEquals(Long.MIN_VALUE, outsideLong.longValue());
        assertFalse(outsideLong.canConvertToLong());
    }

    @Test
    void keepsLastDuplicateValue() throws Exception {
        JsonNode root = JsonExample.parse(fixture("duplicate-keys.json"));
        assertEquals(2, root.required("value").intValue());
    }

    @Test
    void rejectsTrailingTokens() throws Exception {
        String trailing = fixture("trailing-tokens.json");
        assertThrows(JsonProcessingException.class, () -> JsonExample.parse(trailing));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        String malformed = fixture("malformed.json");
        assertThrows(JsonProcessingException.class, () -> JsonExample.parse(malformed));
    }
}
