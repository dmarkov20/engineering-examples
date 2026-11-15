package examples.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

import java.nio.file.Files;
import java.nio.file.Path;

public class JsonExample {
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    static JsonElement parse(String json) {
        return GSON.fromJson(json, JsonElement.class);
    }

    static String describe(JsonObject root) {
        String name = root.get("name").getAsString();
        int version = root.get("version").getAsInt();
        boolean active = root.get("active").getAsBoolean();
        String language = root.getAsJsonObject("metadata").get("language").getAsString();
        String feature = root.getAsJsonArray("features").get(1).getAsString();
        return String.format("%s %d (active=%s, language=%s, feature=%s)",
                name, version, active, language, feature);
    }

    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of("../fixtures/sample.json"));
        System.out.println(describe(parse(json).getAsJsonObject()));
    }
}
