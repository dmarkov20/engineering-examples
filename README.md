# Engineering examples

Small software engineering examples for learning, experimentation, and
technical articles. Each example aims to be independently runnable,
reproducible, and clear enough to explain both the API and its limitations.

The repository covers software engineering across languages. Future topics
may include C/C++, concurrency, runtime internals, performance, and diagnostics.

## Available examples

- [JSON parsing in Java](java/json/README.md) compares bundled Nashorn,
  standalone Nashorn, Jackson, Gson, Jakarta JSON-P with Parsson, and the
  JEP 540 Simple JSON API incubator. Shared fixtures and tests demonstrate
  navigation, errors, nulls, duplicate keys, and numeric conversions.

## Quick start

From the repository root, with JDK 17+ and Maven 3.9.x on your path:

```sh
java/json/examples.sh run jackson
java/json/examples.sh test jackson
```

The four Maven examples use JDK 17+. Bundled Nashorn requires JDK 8u60 or
later in the JDK 8 line. JEP 540 uses the verified OpenJDK
`28-ea+23-1886` incubator build. The runner selects these additional JDKs
through `JDK8_HOME` and `JDK28_HOME`.

See the [JSON setup and full comparison](java/json/README.md#run-from-the-repository-root)
and the detailed [bundled Nashorn](java/json/nashorn-jdk8/README.md) and
[JEP 540](java/json/jep540/README.md) instructions.
