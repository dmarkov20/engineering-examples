import jdk.incubator.json.Json;
import jdk.incubator.json.JsonNull;
import jdk.incubator.json.JsonParseException;
import jdk.incubator.json.JsonValue;
import jdk.incubator.json.JsonValueException;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonExampleTest {
    public static void main(String[] args) throws Exception {
        if (!JsonExampleTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Run tests with java -ea");
        }

        sample();
        emptyContainers();
        nestedValuesAndNull();
        numbers();
        assertParseError("malformed.json");
        assertParseError("duplicate-keys.json");
        assertParseError("trailing-tokens.json");
        System.out.println("JEP 540 checks passed");
    }

    private static JsonValue fixture(String name) throws Exception {
        return Json.parse(Files.readString(Path.of("../fixtures", name)));
    }

    private static void sample() throws Exception {
        assert "OpenJDK 28 (active=true, language=Java, feature=JVM)"
                .equals(JsonExample.describe(fixture("sample.json")));
    }

    private static void emptyContainers() throws Exception {
        assert fixture("empty-object.json").asMap().isEmpty();
        assert fixture("empty-array.json").asList().isEmpty();
    }

    private static void nestedValuesAndNull() throws Exception {
        JsonValue items = fixture("edge-cases.json").get("items");
        assert "Quote: \" and newline:\nDublin ☘ Ω 🚀".equals(items.get(0).get("text").asString());
        JsonValue second = items.get(1);
        assert second.tryGet("value").orElseThrow() instanceof JsonNull;
        assert second.tryGet("missing").isEmpty();
        assertValueError(() -> second.get("missing"));
    }

    private static void numbers() throws Exception {
        JsonValue root = fixture("edge-cases.json");
        assert root.get("integer").asInt() == -42;
        assert root.get("largeInteger").asLong() == 9007199254740993L;
        JsonValue decimal = root.get("decimal");
        assert decimal.asDouble() == 0.12345678901234568;
        assert root.get("largeInteger").asDouble() == 9007199254740992.0;
        assertValueError(decimal::asLong);
        JsonValue outsideLong = root.get("outsideLong");
        assertValueError(outsideLong::asLong);
        assert new BigInteger(outsideLong.toString()).equals(new BigInteger("9223372036854775808"));
    }

    private static void assertValueError(Runnable access) {
        try {
            access.run();
            throw new AssertionError("Expected JsonValueException");
        } catch (JsonValueException expected) {
            // Missing members and inexact integral conversions raise this exception.
        }
    }

    private static void assertParseError(String name) throws Exception {
        try {
            fixture(name);
            throw new AssertionError("Expected JsonParseException for " + name);
        } catch (JsonParseException expected) {
            // The complete fixture is rejected.
        }
    }
}
