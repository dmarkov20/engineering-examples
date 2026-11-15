package examples.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public class JsonExample {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    static JsonNode parse(String json) throws JsonProcessingException {
        return MAPPER.readTree(json);
    }

    static String describe(JsonNode root) {
        String name = root.required("name").textValue();
        int version = root.required("version").intValue();
        boolean active = root.required("active").booleanValue();
        String language = root.required("metadata").required("language").textValue();
        String feature = root.required("features").required(1).textValue();
        return String.format("%s %d (active=%s, language=%s, feature=%s)",
                name, version, active, language, feature);
    }

    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of("../fixtures/sample.json"));
        System.out.println(describe(parse(json)));
    }
}
