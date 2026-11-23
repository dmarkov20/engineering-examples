package examples.archives;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Extraction {
    private final Path root;
    private final boolean posix;
    private final List<Path> directories = new ArrayList<>();
    private final Map<String, Integer> storedModes = new HashMap<>();

    Extraction(Path parent, String prefix) throws IOException {
        posix = Modes.supported(parent);
        root = (posix ? Files.createTempDirectory(parent, prefix,
                PosixFilePermissions.asFileAttribute(Modes.permissions(0700)))
                : Files.createTempDirectory(parent, prefix)).toRealPath();
    }

    Path root() {
        return root;
    }

    Integer recordedMode(String name) {
        return storedModes.get(name);
    }

    private Path target(String name) throws IOException {
        Path relative = root.getFileSystem().getPath(name);
        Path path = root.resolve(relative).normalize();
        if (relative.getRoot() != null || name.contains("\\") || path.equals(root) || !path.startsWith(root)) {
            throw new IOException("Expected a relative entry inside the destination: " + name);
        }
        return path;
    }

    void directory(String name, Integer mode) throws IOException {
        // A tarball made with "tar -C source ." can describe the extraction root.
        if (name.equals(".") || name.equals("./")) {
            return;
        }
        Path path = Files.createDirectories(target(name));
        if (mode != null) {
            storedModes.put(ArchiveFixture.name(root, path), mode);
            directories.add(path);
        }
    }

    Path fileTarget(String name) throws IOException {
        Path path = target(name);
        Files.createDirectories(path.getParent());
        return path;
    }

    void file(String name, InputStream data, Integer mode) throws IOException {
        Path path = fileTarget(name);
        Files.copy(data, path);
        if (mode != null) {
            storedModes.put(ArchiveFixture.name(root, path), mode);
            if (posix) {
                Files.setPosixFilePermissions(path, Modes.permissions(mode));
            }
        }
    }

    void finishDirectories() throws IOException {
        if (posix) {
            directories.sort(Comparator.comparingInt(Path::getNameCount).reversed());
            for (Path path : directories) {
                int mode = storedModes.get(ArchiveFixture.name(root, path));
                Files.setPosixFilePermissions(path, Modes.permissions(mode));
            }
        }
    }
}
