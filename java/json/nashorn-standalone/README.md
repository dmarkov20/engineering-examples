# Standalone Nashorn

This example uses OpenJDK's standalone Nashorn 15.7. The engine works on Java
11 and later. This Maven project targets JDK 17 and uses Maven 3.9.x.


From `java/json/nashorn-standalone`, compile and run the demo, then run its JUnit tests:

```sh
mvn compile exec:java
mvn test
```

Verified both commands on macOS arm64 with Maven 3.9.11 and Oracle JDK
25.0.5.0.3.

`NashornScriptEngineFactory` selects the standalone engine explicitly. The
example puts the input in JSR 223 bindings, then evaluates a fixed script
that calls `JSON.parse()` and `Java.asJSONCompatible()`. Java receives objects
as map views and arrays as list views. Use `containsKey()` to distinguish an
explicit null from an absent property.

Nashorn applies JavaScript number semantics. The large integer rounds at
parse time, so converting the resulting `Number` to `long` cannot recover it.
A syntax error is reported to Java as `ScriptException`.

The Maven dependency includes the engine and brings in ASM dependencies.
This demonstrates an engine-based migration path for existing Nashorn code.
The `JSON.parse()` call materializes a complete tree.
[OpenJDK Nashorn project](https://github.com/openjdk/nashorn)
