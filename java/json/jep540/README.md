# JEP 540 Simple JSON API

This experimental example uses the JDK 28 incubator module
`jdk.incubator.json`. It was compiled and run on macOS arm64 with OpenJDK
`28-ea+23-1886`. The API may change between builds.
[OpenJDK JEP 540](https://openjdk.org/jeps/540)

From `java/json/jep540`, with that early-access build on your path:

```sh
java -version
java --describe-module jdk.incubator.json
mkdir -p out
javac --add-modules jdk.incubator.json -encoding UTF-8 -d out JsonExample.java JsonExampleTest.java
java --add-modules jdk.incubator.json -cp out JsonExample
java --add-modules jdk.incubator.json -ea -cp out JsonExampleTest
```

Incubator warnings are expected. The example requires the module flag at
compile and run time. From the repository root, set `JDK28_HOME` to the
verified JDK home and run `java/json/examples.sh test jep540`. The runner
checks the build before compiling.

`Json.parse()` yields an immutable `JsonValue` tree. Navigate by object member
or array index with `get()`, then convert with methods such as `asString()` and
`asInt()`. `tryGet()` distinguishes absence from a present `JsonNull`.

## Numeric representation and conversion

The API documentation in `lib/src.zip` shipped with OpenJDK
`28-ea+23-1886` describes `JsonNumber` as an arbitrary-precision decimal
value. For a number created by parsing, `JsonNumber.toString()` preserves
the number's original text regardless of its precision or range. This is a
number-specific guarantee, distinct from general `JsonValue.toString()`
serialization.

Converting that value to a Java primitive imposes the primitive's limits.
`asInt()` and `asLong()` require a whole number within the destination type's
range. `asDouble()` converts as if by `Double.parseDouble()`, allowing
binary64 rounding but requiring a finite result. For example,
`9007199254740993` converts exactly with `asLong()` and rounds to
`9007199254740992` with `asDouble()`. The parsed JSON value retains its
precision when a conversion rounds or throws.

The existing fixture also contains `9223372036854775808`, one above
`Long.MAX_VALUE`. Its `asLong()` conversion throws `JsonValueException`.
The regression test then constructs `new BigInteger(value.toString())`
and verifies that the original integer remains exact. For decimal or
exponent notation, `new BigDecimal(value.toString())` supports precise
decimal conversion, subject to Java's `BigDecimal` limits. These conversions
use only JDK classes.

These semantics were checked against `JsonNumber.java` and `JsonValue.java`
in the verified build's bundled source and exercised by the tests. The
online EA documentation advances to newer builds over time.

The parser rejects malformed JSON, duplicate object names, and a second
root value with `JsonParseException`. An incompatible value conversion raises
`JsonValueException`. The incubator supports parsing and generating JSON
trees. Object binding and streaming are outside its scope.
[OpenJDK 28 early-access API documentation](https://download.java.net/java/early_access/jdk28/docs/api/jdk.incubator.json/jdk/incubator/json/package-summary.html)
