# Gson tree parsing

This example uses the maintained third-party Gson 2.15.0 library. Run it with
JDK 17 or newer and Maven 3.9.x.


From `java/json/gson`, compile and run the demo, then run its JUnit tests:

```sh
mvn compile exec:java
mvn test
```

Verified both commands on macOS arm64 with Maven 3.9.11 and Oracle JDK
25.0.5.0.3.

A `Gson` configured with `Strictness.STRICT` parses to a `JsonElement` tree.
Use `getAsJsonObject()` and `getAsJsonArray()` to navigate and scalar getters
such as `getAsInt()` to convert values. Scalar getters can coerce types, so
check the primitive's type separately when validating input. `JsonObject.get()`
returns Java null for an absent member and `JsonNull` for an explicit null.

The tests retrieve the decimal as `BigDecimal` and the large integer as
`long`. The strict string parser rejects a trailing comma and trailing root
content with `JsonSyntaxException`. In the duplicate-key fixture the tree
keeps the last value, even with strict parsing.

Gson supplies Java object binding and the `JsonReader` and `JsonWriter`
streaming APIs alongside the tree model.
[Gson documentation](https://google.github.io/gson/)
