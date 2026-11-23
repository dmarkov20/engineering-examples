package examples.archives;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class JdkZipTest {
    private static final Map<String, Integer> CHANGED_MODES = Map.of(
            ArchiveFixture.SCRIPT, 0700, ArchiveFixture.CONFIG, 0640, "bin", 0710);

    public static void main(String[] args) throws IOException {
        Path work = ArchiveFixture.workDirectory("jdk-zip-test");
        JdkZip.demonstrate(ArchiveFixture.source(work), work);

        Path source = ArchiveFixture.create(work.resolve("changed-input"));
        if (Modes.supported(source)) {
            for (var entry : CHANGED_MODES.entrySet()) {
                Files.setPosixFilePermissions(source.resolve(entry.getKey()), Modes.permissions(entry.getValue()));
            }
        }
        checkTree("Separate ZIP source", source);
        Path plain = work.resolve("without-modes.zip");
        JdkZip.write(source, plain);
        Path stored = Files.copy(plain, work.resolve("with-modes.zip"));
        JdkZip.storePermissions(source, stored);
        Files.move(source, work.resolve("moved-input"));

        checkStoredZip(stored, work);
        checkPlainZip(plain, work);
        System.out.println("JDK ZIP round trip, changed-mode and absent-mode checks passed");
    }

    private static void checkStoredZip(Path archive, Path work) throws IOException {
        var restored = JdkZip.extractFileSystem(archive, work, JdkZip.FileCopy.RESTORE_MODE);
        checkTree("Annotated ZIP, explicit restore", restored.root());
        if (Modes.supported(work)) {
            for (var entry : CHANGED_MODES.entrySet()) {
                String name = entry.getKey();
                ArchiveFixture.expect(entry.getValue(), restored.recordedMode(name), name + " stored mode");
                ArchiveFixture.expect(entry.getValue(), Modes.read(restored.root().resolve(name)),
                        name + " restored mode");
            }
        } else {
            System.out.println("SKIP changed-mode assertions (no POSIX file attributes)");
        }

        checkTree("Annotated ZIP, stream", JdkZip.extractStream(archive, work).output().root());
        var copied = JdkZip.extractFileSystem(archive, work, JdkZip.FileCopy.COPY_ATTRIBUTES);
        checkTree("Annotated ZIP, default-view copy (provider observation)", copied.root());
        var posixCopy = JdkZip.extractFileSystem(archive, work, JdkZip.FileCopy.COPY_ATTRIBUTES_WITH_POSIX_VIEW);
        checkTree("Annotated ZIP, POSIX-view copy (provider observation)", posixCopy.root());
    }

    private static void checkPlainZip(Path archive, Path work) throws IOException {
        try (var fs = FileSystems.newFileSystem(archive, Map.of("enablePosixFileAttributes", true))) {
            Path root = fs.getPath("/");
            for (String name : CHANGED_MODES.keySet()) {
                ArchiveFixture.expect(null, Files.getAttribute(root.resolve(name), "zip:permissions"),
                        name + " absent zip:permissions");
            }
            checkTree("Plain ZIP, POSIX view inside archive (provider observation)", root);
        }

        var restored = JdkZip.extractFileSystem(archive, work, JdkZip.FileCopy.RESTORE_MODE);
        checkTree("Plain ZIP, explicit restore", restored.root());
        if (Modes.supported(work)) {
            Integer ordinaryFile = Modes.read(Files.createFile(work.resolve("ordinary-file")));
            for (String name : List.of(ArchiveFixture.SCRIPT, ArchiveFixture.CONFIG)) {
                ArchiveFixture.expect(ordinaryFile, Modes.read(restored.root().resolve(name)),
                        name + " ordinary creation mode");
            }
            Integer ordinaryDir = Modes.read(Files.createDirectory(work.resolve("ordinary-dir")));
            ArchiveFixture.expect(ordinaryDir, Modes.read(restored.root().resolve("bin")), "bin creation mode");
        } else {
            System.out.println("SKIP creation mode comparisons (no POSIX file attributes)");
        }
        var copied = JdkZip.extractFileSystem(archive, work, JdkZip.FileCopy.COPY_ATTRIBUTES_WITH_POSIX_VIEW);
        checkTree("Plain ZIP, POSIX-view copy (provider observation)", copied.root());
    }

    private static void checkTree(String label, Path root) throws IOException {
        ArchiveFixture.checkContents(root);
        System.out.printf("%s | script / config / bin: %s / %s / %s%n", label,
                Modes.format(Modes.read(root.resolve(ArchiveFixture.SCRIPT))),
                Modes.format(Modes.read(root.resolve(ArchiveFixture.CONFIG))),
                Modes.format(Modes.read(root.resolve("bin"))));
    }

    private JdkZipTest() {}
}
