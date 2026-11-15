package examples.json;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonExample {
    static JsonValue parse(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readValue();
        }
    }

    static String describe(JsonObject root) {
        String name = root.getString("name");
        int version = root.getJsonNumber("version").intValueExact();
        boolean active = root.getBoolean("active");
        String language = root.getJsonObject("metadata").getString("language");
        String feature = root.getJsonArray("features").getString(1);
        return String.format("%s %d (active=%s, language=%s, feature=%s)",
                name, version, active, language, feature);
    }

    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of("../fixtures/sample.json"));
        System.out.println(describe(parse(json).asJsonObject()));
    }
}
