# JSON parsing in Java

Six independent examples read the same [UTF-8 fixture](fixtures/sample.json)
and navigate an object member, a nested object, and an array index. The
fixture's version field is sample data. The required toolchains are below.

## Run from the repository root

For the four Maven modules, put Maven 3.9.x on your `PATH` and use JDK 17 or
newer for Maven. If you set `JAVA_HOME`, point it to that JDK.

```sh
java/json/examples.sh run jackson
java/json/examples.sh test json-p
```

Set the two extra JDK homes to run the complete comparison. On macOS, use
the `Contents/Home` directory inside each JDK bundle.

```sh
export JDK8_HOME=/path/to/jdk8
export JDK28_HOME=/path/to/jdk-28-ea-23
java/json/examples.sh run all
java/json/examples.sh test all
```

The runner resolves fixture paths by launching each command in its module
directory. `all` attempts each module and returns a failure status if any
module fails. You can also run the Maven tests together with
`mvn -f java/json/pom.xml test`. Each demo prints:

```text
OpenJDK 28 (active=true, language=Java, feature=JVM)
```

## APIs and JDK compatibility

| Example | Toolchain for this repository | API design and trade-offs |
| --- | --- | --- |
| [Bundled Nashorn](nashorn-jdk8/README.md) | JDK 8u60 or later in the JDK 8 line | JSR 223 runs a fixed JavaScript parser call. Java sees map and list views, with casts at lookup sites and JavaScript number semantics. Bundled Nashorn was removed in JDK 15. |
| [Standalone Nashorn](nashorn-standalone/README.md) | JDK 17+ for this project. Nashorn 15.7 itself supports Java 11+. | The standalone engine carries the same JavaScript semantics to modern JDKs. It adds the Nashorn and ASM dependencies. Both Nashorn demos parse the full tree through an engine. |
| [Jackson](jackson/README.md) | JDK 17+, Jackson 2.21.8 | `JsonNode` offers required and optional lookup styles. Parser and mapper flags control behavior. Jackson also supports Java object binding and streaming, while scalar tree accessors can narrow values. |
| [Gson](gson/README.md) | JDK 17+, Gson 2.15.0 | `JsonElement` has object, array, primitive, and null variants. Scalar getters can coerce values. Gson also offers object binding and streaming. |
| [Jakarta JSON-P](json-p/README.md) | JDK 17+, JSON-P 2.1.3, Parsson 1.1.9 | The Jakarta API separates the typed tree and streaming contracts from the runtime provider. Exact number conversion is part of the tree API. Object binding is a separate Jakarta specification. |
| [JEP 540](jep540/README.md) | OpenJDK `28-ea+23-1886`, with `--add-modules jdk.incubator.json` | The incubator provides an immutable tree with typed accessors and exact integral conversions. Its scope is tree parsing and generation. The runner checks the verified build before compiling. |

Verification used macOS arm64, Corretto `1.8.0_512-b09` for bundled Nashorn,
Maven 3.9.11 with Oracle JDK `25.0.5.0.3` for the four Maven modules, and the
specified OpenJDK EA build for JEP 540.
[OpenJDK Nashorn removal history](https://openjdk.org/jeps/372)

## Duplicate keys, trailing values, and missing members

RFC 8259 recommends unique object member names for interoperability. Its
grammar describes one root value surrounded by optional whitespace. The
[duplicate-key fixture](fixtures/duplicate-keys.json) repeats `value` with
`1` and `2`. The [trailing-token fixture](fixtures/trailing-tokens.json)
contains two complete objects. The [edge-case fixture](fixtures/edge-cases.json)
contains an explicit `null` member alongside an absent member.
[RFC 8259](https://www.rfc-editor.org/rfc/rfc8259.html)

| Parser in this example | Duplicate key | Second root value | Explicit `null` versus absent member |
| --- | --- | --- | --- |
| Bundled and standalone Nashorn `JSON.parse()` | Keeps `2` | Rejects with `ScriptException` | Both lookups give Java `null`. `containsKey()` distinguishes them. |
| Jackson `readTree()` | Keeps `2` with the example's tree settings | Rejects with `FAIL_ON_TRAILING_TOKENS` enabled | `get()` gives a null node or Java `null`. For an absent member, `path()` gives a missing node and `required()` throws. |
| Gson strict `fromJson(String, JsonElement.class)` | Keeps `2` even in `Strictness.STRICT` | Rejects with `JsonSyntaxException` | `get()` gives `JsonNull` or Java `null`. |
| JSON-P `readValue()` with Parsson 1.1.9 | Keeps `2` | Returns the first object with `value: 1` | `get()` gives `JsonValue.NULL` or Java `null`. |
| JEP 540 `Json.parse()` on build 23 | Rejects with `JsonParseException` | Rejects with `JsonParseException` | `tryGet()` contains `JsonNull` or is empty. `get()` throws for an absent member. |

The Parsson result is specific to the provider and the `readValue()` call
shown here. Reading one value does not establish that the complete input is
a valid JSON document. A caller requiring whole-document validation must also
require end of input after optional JSON whitespace, rejecting extra values
and malformed suffixes. See the [JSON-P explanation](json-p/README.md#trailing-input-and-document-validation)
for the approach and buffering considerations. In the Gson tree example, strict syntax and
duplicate-name handling are separate behaviors. All six reject the
[trailing-comma fixture](fixtures/malformed.json).

## Numeric precision and narrowing

The edge-case fixture separates loss at parse time from loss when a caller
requests a narrower type. `9007199254740993` fits in a Java `long` but rounds
to `9007199254740992` in binary64. The fraction is `0.1234567890123456789`.
The out-of-range integer `9223372036854775808` is one above `Long.MAX_VALUE`.

| API | In-range large integer and fraction | Converting to `long` |
| --- | --- | --- |
| Both Nashorn engines | The large integer rounds during parsing. The fraction also rounds to binary64. | `Number.longValue()` returns `0` for the fraction and `Long.MAX_VALUE` for the out-of-range fixture. |
| Jackson | The integer stays exact. `USE_BIG_DECIMAL_FOR_FLOATS` preserves the fraction as `BigDecimal`. | `longValue()` truncates the fraction and wraps the out-of-range fixture to `Long.MIN_VALUE`. Combine `canConvertToExactIntegral()` and `canConvertToLong()` to check before narrowing. |
| Gson | `getAsLong()` preserves the in-range integer. `getAsBigDecimal()` preserves the fraction. | `getAsLong()` truncates or wraps on these fixtures. `getAsBigDecimal().longValueExact()` rejects both inexact conversions. |
| JSON-P | `longValueExact()` preserves the in-range integer and `bigDecimalValue()` preserves the fraction. | `longValue()` truncates or wraps on these fixtures. `longValueExact()` rejects both. |
| JEP 540 on build 23 | The parsed `JsonNumber` retains its decimal text. `asLong()` converts the in-range integer exactly, while `asDouble()` rounds the integer and fraction. | `asLong()` throws `JsonValueException` for the fraction and the out-of-range integer. The original value remains available through `toString()`. |

Jackson, Gson, and JSON-P also retrieve the out-of-range fixture exactly as
a `BigInteger`. On the verified JEP 540 build, the tests construct a
`BigInteger` from the parsed number's preserved text after its `asLong()`
conversion fails. Rejection by an exact conversion describes the destination
type's limits and does not imply that parsing lost the original value.
See the [JEP 540 numeric explanation](jep540/README.md#numeric-representation-and-conversion).
The tests assert each API's observed behavior.
