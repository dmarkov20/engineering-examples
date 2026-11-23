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
- [ZIP and tar.gz archives in Java](java/archives/README.md) shows how to
  create and extract a directory with the JDK ZIP APIs and Apache Commons
  Compress. The examples follow an executable script and a private config
  file from their source permissions through the archive to the unpacked files.

## Quick start

Run these commands from the repository root.

### Parse JSON with Jackson

Use JDK 17 or later and Maven 3.9.x.

```sh
java/json/examples.sh run jackson
java/json/examples.sh test jackson
```

The [JSON guide](java/json/README.md#run-from-the-repository-root) covers
all six examples, including the additional JDKs for
[bundled Nashorn](java/json/nashorn-jdk8/README.md) and
[JEP 540](java/json/jep540/README.md).

### Create and extract a ZIP with the JDK

Use JDK 21 or later. This example runs with `javac` and `java`.

```sh
java/archives/examples.sh run jdk-zip
java/archives/examples.sh test jdk-zip
```

With Maven 3.9.x installed, use `commons-zip` or `tar-gz` to try the Apache
examples, or `all` to run all three. The [archive guide](java/archives/README.md#run-the-examples)
includes the toolchain details and the measured permissions after extraction.
