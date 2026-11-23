package examples.archives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.*;

final class Modes {
    // Each index is the bit position in the octal Unix mode.
    private static final List<PosixFilePermission> BITS = List.of(
            OTHERS_EXECUTE, OTHERS_WRITE, OTHERS_READ,
            GROUP_EXECUTE, GROUP_WRITE, GROUP_READ,
            OWNER_EXECUTE, OWNER_WRITE, OWNER_READ);

    static boolean supported(Path path) throws IOException {
        return Files.getFileStore(path).supportsFileAttributeView(PosixFileAttributeView.class);
    }

    static Integer read(Path path) throws IOException {
        return supported(path) ? bits(Files.getPosixFilePermissions(path)) : null;
    }

    static int bits(Set<PosixFilePermission> permissions) {
        int mode = 0;
        for (int i = 0; i < BITS.size(); i++) {
            if (permissions.contains(BITS.get(i))) {
                mode |= 1 << i;
            }
        }
        return mode;
    }

    static Set<PosixFilePermission> permissions(int mode) {
        var permissions = EnumSet.noneOf(PosixFilePermission.class);
        for (int i = 0; i < BITS.size(); i++) {
            if ((mode & (1 << i)) != 0) {
                permissions.add(BITS.get(i));
            }
        }
        return permissions;
    }

    static String format(Integer mode) {
        return mode == null ? "unavailable" : "%04o".formatted(mode);
    }

    private Modes() {}
}
