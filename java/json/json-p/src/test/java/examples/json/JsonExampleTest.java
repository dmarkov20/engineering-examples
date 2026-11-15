package examples.json;

import org.junit.jupiter.api.Test;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParsingException;

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
                JsonExample.describe(JsonExample.parse(fixture("sample.json")).asJsonObject()));
    }

    @Test
    void readsEmptyContainers() throws Exception {
        assertTrue(JsonExample.parse(fixture("empty-object.json")).asJsonObject().isEmpty());
        assertTrue(JsonExample.parse(fixture("empty-array.json")).asJsonArray().isEmpty());
    }

    @Test
    void readsNestedValuesAndDistinguishesNullFromMissing() throws Exception {
        JsonObject root = JsonExample.parse(fixture("edge-cases.json")).asJsonObject();
        JsonArray items = root.getJsonArray("items");
        assertEquals("Quote: \" and newline:\nDublin ☘ Ω 🚀", items.getJsonObject(0).getString("text"));
        JsonObject second = items.getJsonObject(1);
        assertSame(JsonValue.NULL, second.get("value"));
        assertNull(second.get("missing"));
    }

    @Test
    void preservesIntegersAndDecimalPrecision() throws Exception {
        JsonObject root = JsonExample.parse(fixture("edge-cases.json")).asJsonObject();
        assertEquals(-42, root.getJsonNumber("integer").intValueExact());
        JsonNumber decimal = root.getJsonNumber("decimal");
        assertEquals(new BigDecimal("0.1234567890123456789"), decimal.bigDecimalValue());
        assertEquals(9007199254740993L, root.getJsonNumber("largeInteger").longValueExact());

        assertEquals(0, decimal.longValue());
        assertThrows(ArithmeticException.class, decimal::longValueExact);
        JsonNumber outsideLong = root.getJsonNumber("outsideLong");
        assertEquals(new BigInteger("9223372036854775808"), outsideLong.bigIntegerValueExact());
        assertEquals(Long.MIN_VALUE, outsideLong.longValue());
        assertThrows(ArithmeticException.class, outsideLong::longValueExact);
    }

    @Test
    void keepsLastDuplicateValueWithParsson() throws Exception {
        JsonObject root = JsonExample.parse(fixture("duplicate-keys.json")).asJsonObject();
        assertEquals(2, root.getInt("value"));
    }

    @Test
    void readsOnlyFirstRootValueWithParsson() throws Exception {
        JsonObject root = JsonExample.parse(fixture("trailing-tokens.json")).asJsonObject();
        assertEquals(1, root.getInt("value"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        String malformed = fixture("malformed.json");
        assertThrows(JsonParsingException.class, () -> JsonExample.parse(malformed));
    }
}
