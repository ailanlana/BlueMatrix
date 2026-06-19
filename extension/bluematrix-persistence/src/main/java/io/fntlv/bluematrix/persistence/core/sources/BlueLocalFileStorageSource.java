package io.fntlv.bluematrix.persistence.core.sources;

import br.com.finalcraft.everydatabase.modules.localfile.LocalFileConfig;
import io.fntlv.bluematrix.persistence.core.BlueStorageSpec;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

public interface BlueLocalFileStorageSource extends BlueStorageSource {
    default String getBaseDirectory() {
        return "";
    }

    default boolean isPrettyPrint() {
        return true;
    }

    default long getFsyncEveryMillis() {
        return -1L;
    }

    @Override
    default BlueStorageSpec toSpec(BlueStorageSourceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        String baseDirectory = normalizeBaseDirectory(getBaseDirectory());
        File storagePath = baseDirectory.isEmpty()
                ? context.storageRootDirectory()
                : new File(context.storageRootDirectory(), baseDirectory);
        Optional<Duration> fsyncEvery = getFsyncEveryMillis() > 0
                ? Optional.of(Duration.ofMillis(getFsyncEveryMillis()))
                : Optional.<Duration>empty();
        return BlueStorageSpec.localFile(new LocalFileConfig(
                storagePath.toPath(),
                isPrettyPrint(),
                fsyncEvery
        ));
    }

    default String normalizeBaseDirectory(String baseDirectory) {
        if (baseDirectory == null) {
            return "";
        }
        String normalized = baseDirectory.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        File file = new File(normalized);
        if (file.isAbsolute() || normalized.matches("^[A-Za-z]:[\\\\/].*")) {
            throw new IllegalArgumentException("baseDirectory must be relative: " + baseDirectory);
        }
        String[] segments = normalized.replace('\\', '/').split("/");
        for (String segment : segments) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("baseDirectory cannot contain path traversal: " + baseDirectory);
            }
        }
        return normalized;
    }
}
