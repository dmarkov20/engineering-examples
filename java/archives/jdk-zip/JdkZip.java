package examples.archives;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class JdkZip {
    enum FileCopy { RESTORE_MODE, COPY_ATTRIBUTES, COPY_ATTRIBUTES_WITH_POSIX_VIEW }
    record StreamResult(Extraction output, long sizeBefore, long sizeAfter) {}
    record NamedSummary(long size, String text) {}

    static void write(Path source, Path archive) throws IOException {
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive), UTF_8)) {
            for (Path path : ArchiveFixture.entries(source)) {
                String name = ArchiveFixture.name(source, path);
                boolean directory = ArchiveFixture.isDirectory(path);
                var entry = new ZipEntry(directory ? name + "/" : name);
                entry.setMethod(ZipEntry.DEFLATED);
                zip.putNextEntry(entry);
                if (!directory) {
                    Files.copy(path, zip);
                }
                zip.closeEntry();
            }
        }
    }

    static void storePermissions(Path source, Path archive) throws IOException {
        if (Modes.supported(source)) {
            try (var fs = FileSystems.newFileSystem(archive, Map.of())) {
                // Update children before replacing their directory entries in zipfs.
                for (Path path : ArchiveFixture.entries(source).reversed()) {
                    Path entry = fs.getPath("/", ArchiveFixture.name(source, path));
                    Files.setAttribute(entry, "zip:permissions", Files.getPosixFilePermissions(path));
                }
            }
        }
    }

    static StreamResult extractStream(Path archive, Path parent) throws IOException {
        var out = new Extraction(parent, "stream-");
        long before = -1;
        long after = -1;
        try (var zip = new ZipInputStream(Files.newInputStream(archive), UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    out.directory(name, null);
                } else {
                    long size = entry.getSize();
                    out.file(name, zip, null);  // Files.copy reads this entry to EOF.
                    if (name.equals(ArchiveFixture.SUMMARY)) {
                        before = size;
                        after = entry.getSize();
                    }
                }
                zip.closeEntry();
            }
        }
        return new StreamResult(out, before, after);
    }

    static NamedSummary readSummary(Path archive) throws IOException {
        try (var zip = new ZipFile(archive.toFile(), UTF_8)) {
            ZipEntry entry = zip.getEntry("summary.json");
            if (entry == null) {
                throw new IOException("Missing summary.json");
            }
            long size = entry.getSize();
            try (var data = zip.getInputStream(entry)) {
                return new NamedSummary(size, new String(data.readAllBytes(), UTF_8));
            }
        }
    }

    @SuppressWarnings("unchecked") // The zip view specifies Set<PosixFilePermission> or null.
    private static Integer storedMode(Path entry) throws IOException {
        var permissions = (Set<PosixFilePermission>) Files.getAttribute(entry, "zip:permissions");
        return permissions == null ? null : Modes.bits(permissions);
    }

    static Extraction extractFileSystem(Path archive, Path parent, FileCopy copy) throws IOException {
        var out = new Extraction(parent, copy == FileCopy.RESTORE_MODE ? "zipfs-" : "copy-");
        var env = Map.of("enablePosixFileAttributes", copy == FileCopy.COPY_ATTRIBUTES_WITH_POSIX_VIEW);
        try (var fs = FileSystems.newFileSystem(archive, env)) {
            Path root = fs.getPath("/");
            for (Path entry : ArchiveFixture.entries(root)) {
                String name = ArchiveFixture.name(root, entry);
                Integer mode = copy == FileCopy.RESTORE_MODE ? storedMode(entry) : null;
                if (Files.isDirectory(entry)) {
                    out.directory(name, mode);
                } else if (copy != FileCopy.RESTORE_MODE) {
                    Files.copy(entry, out.fileTarget(name), StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    try (var data = Files.newInputStream(entry)) {
                        out.file(name, data, mode);
                    }
                }
            }
        }
        out.finishDirectories();
        return out;
    }

    static void demonstrate(Path source, Path work) throws IOException {
        Path archive = work.resolve("diagnostics.zip");
        write(source, archive);
        storePermissions(source, archive);
        System.out.println("Archive: " + archive);

        StreamResult stream = extractStream(archive, work);
        NamedSummary named = readSummary(archive);
        System.out.printf("summary.json size: stream before %d, after EOF %d, ZipFile before %d%n",
                stream.sizeBefore(), stream.sizeAfter(), named.size());
        ArchiveFixture.expect(-1L, stream.sizeBefore(), "stream size before reading");
        ArchiveFixture.expect(11L, stream.sizeAfter(), "stream size after EOF");
        ArchiveFixture.expect(new NamedSummary(11L, ArchiveFixture.JSON), named, "named entry");

        ArchiveFixture.checkOutput("ZipInputStream", source, stream.output(), false);
        var restored = extractFileSystem(archive, work, FileCopy.RESTORE_MODE);
        ArchiveFixture.checkOutput("zipfs explicit restore", source, restored, true);
        var copied = extractFileSystem(archive, work, FileCopy.COPY_ATTRIBUTES);
        ArchiveFixture.checkOutput("zipfs COPY_ATTRIBUTES (default view)", source, copied, false);
        var copiedPosix = extractFileSystem(archive, work, FileCopy.COPY_ATTRIBUTES_WITH_POSIX_VIEW);
        ArchiveFixture.checkOutput("zipfs COPY_ATTRIBUTES (POSIX view)", source, copiedPosix, false);

        Path updated = Files.copy(archive, work.resolve("updated.zip"));
        try (var fs = FileSystems.newFileSystem(updated, Map.of())) {
            Files.writeString(fs.getPath("/summary.json"), "{\"ok\":false}");
        }
        NamedSummary edited = readSummary(updated);
        ArchiveFixture.expect("{\"ok\":false}", edited.text(), "updated ZIP");
        ArchiveFixture.expect(named, readSummary(archive), "original ZIP");
        System.out.println("updated.zip summary.json: " + edited.text());
    }

    public static void main(String[] args) throws IOException {
        Path work = ArchiveFixture.workDirectory("jdk-zip");
        demonstrate(ArchiveFixture.source(work), work);
    }

    private JdkZip() {}
}
