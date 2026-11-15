import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class JsonExample {
    private final ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();

    Object parse(String json) throws ScriptException {
        Bindings bindings = engine.createBindings();
        bindings.put("jsonText", json);
        return engine.eval("Java.asJSONCompatible(JSON.parse(jsonText))", bindings);
    }

    static String describe(Object value) {
        Map<?, ?> root = (Map<?, ?>) value;
        Map<?, ?> metadata = (Map<?, ?>) root.get("metadata");
        List<?> features = (List<?>) root.get("features");
        String name = (String) root.get("name");
        int version = ((Number) root.get("version")).intValue();
        boolean active = (Boolean) root.get("active");
        return String.format("%s %d (active=%s, language=%s, feature=%s)",
                name, version, active, metadata.get("language"), features.get(1));
    }

    public static void main(String[] args) throws Exception {
        String json = new String(Files.readAllBytes(Paths.get("../fixtures/sample.json")),
                StandardCharsets.UTF_8);
        System.out.println(describe(new JsonExample().parse(json)));
    }
}
