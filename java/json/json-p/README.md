# Jakarta JSON Processing

This example uses the Jakarta JSON Processing 2.1.3 API and Eclipse Parsson
1.1.9 as the runtime provider. JSON-P is standardized by Jakarta EE. Run the
example with JDK 17 or newer and Maven 3.9.x.


From `java/json/json-p`, compile and run the demo, then run its JUnit tests:

```sh
mvn compile exec:java
mvn test
```

Verified both commands on macOS arm64 with Maven 3.9.11 and Oracle JDK
25.0.5.0.3.

`Json.createReader()` discovers the provider. `JsonReader.readValue()` builds
the tree and the try-with-resources block closes the reader. Navigate with
`JsonObject` and `JsonArray` accessors. `JsonValue.NULL` is the explicit null
value, and an absent member returns Java null through `get()`.

Use `intValueExact()` or `longValueExact()` to reject a fractional or
out-of-range integral conversion. `bigDecimalValue()` retains the decimal's
precision. The malformed fixture raises `JsonParsingException`.
Parsson 1.1.9 keeps the last duplicate member in the duplicate-key fixture.

## Trailing input and document validation

With Eclipse Parsson 1.1.9, this example's `JsonReader.readValue()` call
returns the first object from the two-root fixture
`{"value": 1} {"value": 2}` without reporting the second object. The regression
test records this specific combination of provider version, method, and input.
Other JSON-P implementations or parsing methods may handle trailing input
differently.

Successfully consuming one JSON value establishes that the value was parsed.
It does not establish that the input ends after that value. Whole-document
validation must additionally require end of input after optional JSON
whitespace, rejecting another value or malformed trailing content.

A caller requiring that guarantee can use a streaming parser to consume
exactly one root value and then verify that the same parser reaches the end
of input without further events or syntax errors. Verify this behavior with
the chosen provider. Inspecting the underlying reader separately can miss
characters already buffered by the parser, and calling `readValue()` twice
on a `JsonReader` is unsupported.

JSON-P also supplies a streaming parser and generator. Java object binding is
provided by the separate Jakarta JSON Binding specification.
[Jakarta JSON Processing specification](https://jakarta.ee/specifications/jsonp/2.1/)
