package examples.archives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class TarGz {
    private static TarArchiveEntry initialEntry(Path path, String name, boolean directory, Integer mode)
            throws IOException {
        if (mode == null) {
            // The Path constructor also queries the owner, which is optional on basic stores.
            var entry = new TarArchiveEntry(directory ? name + "/" : name);
            entry.setSize(directory ? 0 : Files.size(path));
            return entry;
        }

        var entry = new TarArchiveEntry(path, name);
        if (name.equals(ArchiveFixture.SCRIPT)) {
            int initial = entry.getMode() & 0777;
            System.out.printf("TAR script before setMode: %04o (source %s)%n", initial, Modes.format(mode));
            ArchiveFixture.expect(0644, initial, "Commons Compress 1.28.0 TAR constructor");
        }
        return entry;
    }

    static void write(Path source, Path archive) throws IOException {
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(archive));
             var tar = new TarArchiveOutputStream(gzip, UTF_8.name())) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar.setAddPaxHeadersForNonAsciiNames(true);
            for (Path path : ArchiveFixture.entries(source)) {
                boolean directory = ArchiveFixture.isDirectory(path);
                String name = ArchiveFixture.name(source, path);
                Integer mode = Modes.read(path);
                TarArchiveEntry entry = initialEntry(path, name, directory, mode);
                if (mode != null) {
                    entry.setMode(mode);
                }
                tar.putArchiveEntry(entry);
                if (!directory) {
                    Files.copy(path, tar);
                }
                tar.closeArchiveEntry();
            }
        }
    }

    static Extraction extract(Path archive, Path parent) throws IOException {
        var out = new Extraction(parent, "tar-");
        try (var gzip = new GZIPInputStream(Files.newInputStream(archive));
             var tar = new TarArchiveInputStream(gzip, UTF_8.name())) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                int mode = entry.getMode() & 0777;
                switch (entry.getLinkFlag()) {
                    case TarConstants.LF_DIR -> out.directory(entry.getName(), mode);
                    case TarConstants.LF_NORMAL, TarConstants.LF_OLDNORM -> out.file(entry.getName(), tar, mode);
                    default -> throw new IOException("Unsupported TAR entry type: " + entry.getName());
                }
            }
        }
        out.finishDirectories();
        return out;
    }

    static void demonstrate(Path source, Path work) throws IOException {
        Path archive = work.resolve("diagnostics.tar.gz");
        write(source, archive);
        System.out.println("Archive: " + archive);
        ArchiveFixture.checkOutput("TarArchiveInputStream", source, extract(archive, work), true);
    }

    public static void main(String[] args) throws IOException {
        Path work = ArchiveFixture.workDirectory("tar-gz");
        demonstrate(ArchiveFixture.source(work), work);
    }

    private TarGz() {}
}
