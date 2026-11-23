package examples.archives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;

public final class CommonsZip {
    static void write(Path source, Path archive) throws IOException {
        try (var zip = new ZipArchiveOutputStream(archive)) {
            for (Path path : ArchiveFixture.entries(source)) {
                boolean directory = ArchiveFixture.isDirectory(path);
                String name = ArchiveFixture.name(source, path);
                var entry = new ZipArchiveEntry(directory ? name + "/" : name);
                Integer mode = Modes.read(path);
                if (mode != null) {
                    int type = directory ? UnixStat.DIR_FLAG : UnixStat.FILE_FLAG;
                    entry.setUnixMode(type | mode);
                }
                zip.putArchiveEntry(entry);
                if (!directory) {
                    Files.copy(path, zip);
                }
                zip.closeArchiveEntry();
            }
        }
    }

    static Extraction extract(Path archive, Path parent) throws IOException {
        var out = new Extraction(parent, "commons-");
        try (ZipFile zip = ZipFile.builder().setPath(archive).get()) {
            var entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                int unixMode = entry.getUnixMode();
                int type = unixMode & UnixStat.FILE_TYPE_FLAG;
                if (type != 0 && type != (entry.isDirectory() ? UnixStat.DIR_FLAG : UnixStat.FILE_FLAG)) {
                    throw new IOException("Unsupported ZIP entry type: " + entry.getName());
                }
                Integer mode = entry.getPlatform() == ZipArchiveEntry.PLATFORM_UNIX ? unixMode & 0777 : null;
                if (entry.isDirectory()) {
                    out.directory(entry.getName(), mode);
                } else {
                    try (var data = zip.getInputStream(entry)) {
                        out.file(entry.getName(), data, mode);
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
        System.out.println("Archive: " + archive);
        ArchiveFixture.checkOutput("Commons ZipFile", source, extract(archive, work), true);
    }

    public static void main(String[] args) throws IOException {
        Path work = ArchiveFixture.workDirectory("commons-zip");
        demonstrate(ArchiveFixture.source(work), work);
    }

    private CommonsZip() {}
}
