package examples.archives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CommonsZipTest {
    @TempDir Path work;

    @Test
    void roundTrip() throws IOException {
        CommonsZip.demonstrate(ArchiveFixture.source(work), work);
    }

    @Test
    void readsTheActualModesEvenWhenTheSourceMoves() throws IOException {
        assumeTrue(Modes.supported(work), "POSIX file attributes required");
        Path source = ArchiveFixture.create(work.resolve("input"));
        Files.setPosixFilePermissions(source.resolve(ArchiveFixture.SCRIPT), Modes.permissions(0711));
        Files.setPosixFilePermissions(source.resolve(ArchiveFixture.CONFIG), Modes.permissions(0640));
        Path archive = work.resolve("changed-modes.zip");
        CommonsZip.write(source, archive);
        Files.move(source, work.resolve("moved"));

        var out = CommonsZip.extract(archive, work);
        ArchiveFixture.checkContents(out.root());
        assertEquals(0711, out.recordedMode(ArchiveFixture.SCRIPT));
        assertEquals(0640, out.recordedMode(ArchiveFixture.CONFIG));
        assertEquals(0711, Modes.read(out.root().resolve(ArchiveFixture.SCRIPT)));
        assertEquals(0640, Modes.read(out.root().resolve(ArchiveFixture.CONFIG)));
    }
}
