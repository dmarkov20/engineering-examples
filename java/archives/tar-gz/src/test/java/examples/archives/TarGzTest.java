package examples.archives;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TarGzTest {
    @TempDir Path work;

    @Test
    void roundTrip() throws IOException {
        TarGz.demonstrate(ArchiveFixture.source(work), work);
    }

    @Test
    void extractsGnuTarRootMarkerAndChildren() throws IOException {
        Path archive = work.resolve("native.tar.gz");
        try (var input = getClass().getResourceAsStream("/native-root-entry.tar.gz")) {
            assertNotNull(input, "GNU tar fixture");
            Files.copy(input, archive);
        }
        var out = TarGz.extract(archive, work);
        Path root = out.root();
        assertArrayEquals("#!/bin/sh\nprintf '%s\\n' 'diagnostics ready'\n".getBytes(UTF_8),
                Files.readAllBytes(root.resolve("bin/run.sh")));
        assertArrayEquals("log.level=info\n".getBytes(UTF_8),
                Files.readAllBytes(root.resolve("conf/app.properties")));

        boolean posix = Modes.supported(root);
        if (posix) {
            assertEquals(0700, Modes.read(root), "private root despite the archive's 0755 marker");
        } else {
            System.out.println("SKIP GNU tar destination modes (no POSIX file attributes)");
        }
        var modes = Map.of("bin", 0710, "bin/run.sh", 0750, "conf", 0700, "conf/app.properties", 0600);
        for (var entry : modes.entrySet()) {
            String name = entry.getKey();
            assertEquals(entry.getValue(), out.recordedMode(name), name + " in archive");
            if (posix) {
                assertEquals(entry.getValue(), Modes.read(root.resolve(name)), name + " after extraction");
            }
        }
    }

    @Test
    void acceptsBareDotDirectoryMarker() throws IOException {
        var out = new Extraction(work, "bare-dot-");
        out.directory(".", 0755);
        out.finishDirectories();
        assertTrue(Files.isDirectory(out.root()));
        if (Modes.supported(out.root())) {
            assertEquals(0700, Modes.read(out.root()));
        }
    }

    @Test
    void roundTripsLongAsciiPath() throws IOException {
        String name = "reports/" + "batch-".repeat(10) + "/" + "detail-".repeat(7) + "summary.txt";
        assertTrue(name.getBytes(UTF_8).length >= 120);
        Path source = work.resolve("long-input");
        Path file = source.resolve(name);
        Files.createDirectories(file.getParent());
        byte[] bytes = "weekly report\n".getBytes(UTF_8);
        Files.write(file, bytes);
        boolean posix = Modes.supported(file);
        if (posix) {
            Files.setPosixFilePermissions(file, Modes.permissions(0640));
        }

        Path archive = work.resolve("long-name.tar.gz");
        TarGz.write(source, archive);
        var out = TarGz.extract(archive, work);
        Path restored = out.root().resolve(name);
        assertArrayEquals(bytes, Files.readAllBytes(restored));
        if (posix) {
            assertEquals(0640, out.recordedMode(name), "full path and archived mode");
            assertEquals(Modes.read(file), Modes.read(restored), "restored source mode");
        } else {
            System.out.println("SKIP long-path mode checks (no POSIX file attributes)");
        }
    }

    @Test
    void keepsContentsOnAStoreWithoutPosixAttributes() throws IOException {
        Path archive = work.resolve("basic-store.tar.gz");
        try (var store = FileSystems.newFileSystem(work.resolve("storage.zip"), Map.of("create", true))) {
            Path root = store.getPath("/");
            Path source = ArchiveFixture.create(root.resolve("source"));
            TarGz.write(source, archive);
            ArchiveFixture.checkOutput("TAR (basic store)", source, TarGz.extract(archive, root), true);
        }
    }

    @Test
    void restoresDirectoryModeAfterWritingItsChildren() throws IOException {
        assumeTrue(Modes.supported(work), "POSIX file attributes required");
        Path source = ArchiveFixture.create(work.resolve("input"));
        Path bin = source.resolve("bin");
        Path archive = work.resolve("restricted-directory.tar.gz");
        Files.setPosixFilePermissions(bin, Modes.permissions(0500));
        try {
            TarGz.write(source, archive);
        } finally {
            Files.setPosixFilePermissions(bin, Modes.permissions(0700));
        }

        var out = TarGz.extract(archive, work);
        Path restoredBin = out.root().resolve("bin");
        try {
            ArchiveFixture.checkContents(out.root());
            assertEquals(0500, out.recordedMode("bin"));
            assertEquals(0500, Modes.read(restoredBin));
            assertEquals(0750, Modes.read(out.root().resolve(ArchiveFixture.SCRIPT)));
        } finally {
            Files.setPosixFilePermissions(restoredBin, Modes.permissions(0700));
        }
    }
}
