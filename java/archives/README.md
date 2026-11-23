# Working with ZIP and tar.gz archives in Java

Three examples pack the same directory. The checks follow `bin/run.sh` (`0750`)
and `conf/app.properties` (`0600`) from the source into the archive and back
onto disk. They also compare every relative path and file byte, including the
11-byte `summary.json`, `empty/`, and `notes/résumé.txt`.

On macOS (APFS) with Oracle JDK 21 and 25 and shell umask `0022`, extracting
the **same ZIP** gave these on-disk modes. Both readers recovered every byte.

| Reader | `bin/run.sh` | `conf/app.properties` |
| --- | --- | --- |
| `ZipInputStream` | `0644` | `0644` |
| zipfs, explicit `zip:permissions` restore | `0750` | `0600` |

## Run the examples

Use JDK 21 or later. The JDK example uses `javac` and `java`. The two Apache
examples use Maven 3.9.x and Commons Compress 1.28.0, pinned in the parent
[POM](pom.xml). Put Maven on `PATH` or use the repository's local
`.toolchains/maven` installation if present.

```sh
cd java/archives
./examples.sh run jdk-zip
./examples.sh run commons-zip
./examples.sh run tar-gz
./examples.sh run all
./examples.sh test all
```

The runner also works as `java/archives/examples.sh` from the repository root.
Each invocation prints its private temporary directory. With `all`, the three
writers read one generated source tree. Each reader extracts using only its
archive and a fresh destination. The runner tries every requested example and
returns a failure status if any fails. To repeat the tests on macOS, select
each installed JDK in turn.

```sh
JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./examples.sh test all
JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./examples.sh test all
```

## Permission controls

The [JDK ZIP test](jdk-zip/JdkZipTest.java) makes a separate source tree with
`0700 / 0640 / 0710` for the script, config, and `bin/`. It writes a plain ZIP,
adds the modes to a copy, and moves the source aside before extraction. On
Oracle JDK `21.0.12.1+1-LTS-4` and `25.0.5.0.3+1-LTS-2` on macOS 26.6.2
(25G83) arm64, APFS, umask `0022`, the results were identical.

| ZIP modes | Reader | Script | Config | `bin/` |
| --- | --- | --- | --- | --- |
| Stored | Explicit restore | `0700` | `0640` | `0710` |
| Stored | Stream | `0644` | `0644` | `0755` |
| Stored | Copy, default view | `0644` | `0644` | `0755` |
| Stored | Copy, POSIX view | `0700` | `0640` | `0755` |
| Absent | Explicit restore | `0644` | `0644` | `0755` |
| Absent | Copy, POSIX view | `0777` | `0777` | `0755` |

The [ZIP reader](jdk-zip/JdkZip.java) preserves a missing `zip:permissions`
value as `null`. The [extraction helper](shared/src/main/java/examples/archives/Extraction.java)
calls `Files.copy(data, path)` first, then `Files.setPosixFilePermissions` when
a mode is present and the destination supports POSIX attributes. On the plain
ZIP this left both files at the ordinary control file's `0644`. With the POSIX
view enabled and `defaultPermissions` unset, the view supplied `0777` for all
three entries. `COPY_ATTRIBUTES` copied that value onto the two regular files.
Both copy variants created directories separately. These copy and fallback
values are local provider measurements. JDK 14 introduced zipfs entry
permissions. JDK 25 added `accessMode` for opening the whole ZIP file system
read-only or read-write.

The [TAR tests](tar-gz/src/test/java/examples/archives/TarGzTest.java) pass a
[GNU tar fixture](tar-gz/src/test/resources/native-root-entry.tar.gz) through
the production reader. Its `./` header carries `0755`. The destination stayed
`0700`, with `0710` for `bin/`, `0750` for the script, `0700` for `conf/`,
and `0600` for the config. File modes are applied after copying and directory
modes after writing their children. The writer enables POSIX pax long paths
and non-ASCII names. A 129-byte ASCII path round-tripped with its complete
name, file bytes, and `0640` mode. On a store without POSIX attributes, the
content tests still run and mode checks are reported as skipped.

To inspect the archives independently, set `work` to the **Work directory**
printed by `./examples.sh run all`.

```sh
work=/path/from/the/runner
zipinfo -l "$work"/jdk-zip-*/diagnostics.zip
zipinfo -l "$work"/commons-zip-*/diagnostics.zip
tar -tvzf "$work"/tar-gz-*/diagnostics.tar.gz
```

## Round-trip results for the article fixture

All three archives stored `0750 / 0600` for the original script and config.
Each row shows the measured modes after extraction under the conditions above.
The two copy rows cover regular files. All content checks passed. The Maven
examples used Maven 3.9.11 and Commons Compress 1.28.0.

| Writer | Reader | Extracted script / config |
| --- | --- | --- |
| JDK ZIP + zipfs modes | [JDK ZIP stream](jdk-zip/JdkZip.java) | `0644 / 0644` |
| JDK ZIP + zipfs modes | zipfs, explicit restore | `0750 / 0600` |
| JDK ZIP + zipfs modes | `COPY_ATTRIBUTES`, default view | `0644 / 0644` |
| JDK ZIP + zipfs modes | `COPY_ATTRIBUTES`, POSIX view | `0750 / 0600` |
| Commons ZIP | [Commons Compress ZipFile](commons-zip/src/main/java/examples/archives/CommonsZip.java) | `0750 / 0600` |
| TAR + GZIP | [TarArchiveInputStream](tar-gz/src/main/java/examples/archives/TarGz.java) | `0750 / 0600` |

For the 11-byte `summary.json`, the streaming size was `-1` before reading and
`11` at EOF. Named lookup reported `11` before reading. The [ZIP update](jdk-zip/JdkZip.java#L139-L146)
writes `{"ok":false}` to a separate archive. The TAR path constructor initially
gave the script `0644`. The writer reads its source mode, sets it on the entry,
and then writes the TAR header.
