import jdk.incubator.json.Json;
import jdk.incubator.json.JsonValue;

import java.nio.file.Files;
import java.nio.file.Path;

public class JsonExample {
    static String describe(JsonValue root) {
        String name = root.get("name").asString();
        int version = root.get("version").asInt();
        boolean active = root.get("active").asBoolean();
        String language = root.get("metadata").get("language").asString();
        String feature = root.get("features").get(1).asString();
        return String.format("%s %d (active=%s, language=%s, feature=%s)",
                name, version, active, language, feature);
    }

    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of("../fixtures/sample.json"));
        System.out.println(describe(Json.parse(json)));
    }
}
