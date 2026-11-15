# Bundled Nashorn on JDK 8

This historical example uses the Nashorn engine bundled with JDK 8. Use
JDK 8u60 or later in the JDK 8 line for `Java.asJSONCompatible()`. No external
dependencies or build tool are required.

From `java/json/nashorn-jdk8`, with JDK 8's `java` and `javac` on your path:

```sh
java -version
mkdir -p out
javac -encoding UTF-8 -d out JsonExample.java JsonExampleTest.java
java -cp out JsonExample
java -ea -cp out JsonExampleTest
```

Verified on macOS arm64 with Amazon Corretto 8.512.09.1
(`1.8.0_512-b09`).

The JSR 223 engine receives the JSON as a bound variable. The script calls
`JSON.parse()`, then `Java.asJSONCompatible()` exposes JavaScript objects and
arrays to Java as `Map` and `List` views. `containsKey()` distinguishes a
present null from an absent member.

JavaScript binary64 number semantics round the large-integer fixture before
Java sees it. Convert the returned `Number` for ordinary values such as the
sample version. Malformed JSON raises `ScriptException`. This route starts a
JavaScript engine and builds the complete value in memory.
[OpenJDK Nashorn extension documentation](https://wiki.openjdk.org/display/Nashorn/Nashorn+extensions)
