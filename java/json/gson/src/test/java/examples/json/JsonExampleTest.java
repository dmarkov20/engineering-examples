package examples.json;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

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
                JsonExample.describe(JsonExample.parse(fixture("sample.json")).getAsJsonObject()));
    }

    @Test
    void readsEmptyContainers() throws Exception {
        assertTrue(JsonExample.parse(fixture("empty-object.json")).getAsJsonObject().isEmpty());
        assertTrue(JsonExample.parse(fixture("empty-array.json")).getAsJsonArray().isEmpty());
    }

    @Test
    void readsNestedValuesAndDistinguishesNullFromMissing() throws Exception {
        JsonArray items = JsonExample.parse(fixture("edge-cases.json")).getAsJsonObject().getAsJsonArray("items");
        assertEquals("Quote: \" and newline:\nDublin ☘ Ω 🚀",
                items.get(0).getAsJsonObject().get("text").getAsString());
        JsonObject second = items.get(1).getAsJsonObject();
        assertTrue(second.get("value").isJsonNull());
        assertNull(second.get("missing"));
    }

    @Test
    void preservesIntegersAndDecimalPrecisionWhenRequested() throws Exception {
        JsonObject root = JsonExample.parse(fixture("edge-cases.json")).getAsJsonObject();
        assertEquals(-42, root.get("integer").getAsInt());
        BigDecimal decimal = root.get("decimal").getAsBigDecimal();
        assertEquals(new BigDecimal("0.1234567890123456789"), decimal);
        assertEquals(9007199254740993L, root.get("largeInteger").getAsLong());

        assertEquals(0, root.get("decimal").getAsLong());
        assertThrows(ArithmeticException.class, decimal::longValueExact);
        assertEquals(new BigInteger("9223372036854775808"), root.get("outsideLong").getAsBigInteger());
        assertEquals(Long.MIN_VALUE, root.get("outsideLong").getAsLong());
        assertThrows(ArithmeticException.class, () -> root.get("outsideLong").getAsBigDecimal().longValueExact());
    }

    @Test
    void keepsLastDuplicateValueInStrictTreeParsing() throws Exception {
        JsonObject root = JsonExample.parse(fixture("duplicate-keys.json")).getAsJsonObject();
        assertEquals(2, root.get("value").getAsInt());
    }

    @Test
    void rejectsTrailingTokens() throws Exception {
        String trailing = fixture("trailing-tokens.json");
        assertThrows(JsonSyntaxException.class, () -> JsonExample.parse(trailing));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        String malformed = fixture("malformed.json");
        assertThrows(JsonSyntaxException.class, () -> JsonExample.parse(malformed));
    }
}
