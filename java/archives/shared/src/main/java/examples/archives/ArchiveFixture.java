package examples.archives;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ArchiveFixture {
    static final String SCRIPT = "bin/run.sh";
    static final String CONFIG = "conf/app.properties";
    static final String SUMMARY = "summary.json";
    static final String JSON = "{\"ok\":true}";

    private static final Map<String, String> FILES = Map.of(
            SCRIPT, "#!/bin/sh\nprintf '%s\\n' 'diagnostics ready'\n",
            CONFIG, "log.level=info\n",
            SUMMARY, JSON,
            "notes/résumé.txt", "Résumé des diagnostics.\n");
    private static final Set<String> DIRECTORIES = Set.of("bin", "conf", "notes", "empty");

    // The runner calls this once, even when several modules use the same source tree.
    public static void main(String[] args) throws IOException {
        Path source = create(Path.of(args[0]));
        System.out.println("Source directory: " + source);
    }

    static Path create(Path root) throws IOException {
        Files.createDirectory(root);
        for (String name : DIRECTORIES) {
            Files.createDirectory(root.resolve(name));
        }
        for (var file : FILES.entrySet()) {
            Files.writeString(root.resolve(file.getKey()), file.getValue(), StandardCharsets.UTF_8);
        }
        if (Modes.supported(root)) {
            Files.setPosixFilePermissions(root.resolve(SCRIPT), Modes.permissions(0750));
            Files.setPosixFilePermissions(root.resolve(CONFIG), Modes.permissions(0600));
        }
        return root;
    }

    static Path workDirectory(String module) throws IOException {
        String batch = System.getProperty("archives.work");
        return batch == null ? Files.createTempDirectory(module + "-")
                : Files.createTempDirectory(Path.of(batch), module + "-");
    }

    static Path source(Path work) throws IOException {
        String batch = System.getProperty("archives.work");
        Path source = batch == null ? create(work.resolve("source")) : Path.of(batch).resolve("source");
        checkContents(source);
        if (Modes.supported(source)) {
            expect(0750, Modes.read(source.resolve(SCRIPT)), SCRIPT + " source mode");
            expect(0600, Modes.read(source.resolve(CONFIG)), CONFIG + " source mode");
        } else {
            System.out.println("SKIP source mode checks (no POSIX file attributes)");
        }
        System.out.println("Source script / config: " + modePair(source));
        return source;
    }

    static List<Path> entries(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> !path.equals(root)).sorted().toList();
        }
    }

    static String name(Path root, Path path) {
        return root.relativize(path).toString().replace(root.getFileSystem().getSeparator(), "/");
    }

    static boolean isDirectory(Path path) throws IOException {
        var attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() && !attributes.isRegularFile()) {
            throw new IOException("Expected a regular file or directory: " + path);
        }
        return attributes.isDirectory();
    }

    static void checkContents(Path root) throws IOException {
        var expected = new HashSet<>(DIRECTORIES);
        expected.addAll(FILES.keySet());
        var found = new HashSet<String>();
        for (Path path : entries(root)) {
            String name = name(root, path);
            found.add(name);
            if (DIRECTORIES.contains(name)) {
                expect(true, Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS), name);
            } else {
                expect(true, FILES.containsKey(name), "unexpected entry " + name);
                expect(true, Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), name);
                byte[] bytes = FILES.get(name).getBytes(StandardCharsets.UTF_8);
                expect(true, Arrays.equals(bytes, Files.readAllBytes(path)), name + " bytes");
            }
        }
        expect(expected, found, "relative paths");
    }

    static void checkOutput(String reader, Path source, Extraction out, boolean preservesModes)
            throws IOException {
        checkContents(out.root());
        if (preservesModes) {
            System.out.printf("%s archive script / config: %s / %s%n", reader,
                    Modes.format(out.recordedMode(SCRIPT)), Modes.format(out.recordedMode(CONFIG)));
            if (Modes.supported(source) && Modes.supported(out.root())) {
                for (Path path : entries(source)) {
                    String name = name(source, path);
                    Integer original = Modes.read(path);
                    expect(original, out.recordedMode(name), name + " archived mode");
                    expect(original, Modes.read(out.root().resolve(name)), name + " extracted mode");
                }
            } else {
                System.out.printf("SKIP %s mode checks (no POSIX file attributes)%n", reader);
            }
        }
        System.out.printf("%s extracted script / config: %s | content checks passed%n",
                reader, modePair(out.root()));
    }

    private static String modePair(Path root) throws IOException {
        return Modes.format(Modes.read(root.resolve(SCRIPT))) + " / "
                + Modes.format(Modes.read(root.resolve(CONFIG)));
    }

    static void expect(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private ArchiveFixture() {}
}
