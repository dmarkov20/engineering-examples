# Jackson tree parsing

This example uses the maintained Jackson 2.x API with `jackson-databind`
2.21.8. Run it with JDK 17 or newer and Maven 3.9.x.


From `java/json/jackson`, compile and run the demo, then run its JUnit tests:

```sh
mvn compile exec:java
mvn test
```

Verified both commands on macOS arm64 with Maven 3.9.11 and Oracle JDK
25.0.5.0.3.

`ObjectMapper.readTree()` returns a `JsonNode` tree. `required()` checks that a
member or index exists. The scalar accessors used here assume the sample's
types, so validate node types separately for arbitrary input. `get()` returns
Java null for an absent member and a null node for explicit JSON null.
`path()` returns a missing node for an absent member.

`USE_BIG_DECIMAL_FOR_FLOATS` preserves the decimal fixture's precision.
`FAIL_ON_TRAILING_TOKENS` rejects content after the first value. Invalid syntax
raises `JsonProcessingException`. The tests cover both a trailing comma and a
second root value. With the settings shown here, the last duplicate tree
member wins.

Databind pulls in Jackson Core and Jackson Annotations. The same library
supports mapping Java objects, and Jackson Core supplies a streaming API for
incremental reads and writes.
[Jackson 2.21 release notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21)
