package io.fntlv.bluematrix.persistence.core.sources;

import java.io.File;

public final class BlueStorageSourceContext {
    private final File storageRootDirectory;

    public BlueStorageSourceContext(File storageRootDirectory) {
        if (storageRootDirectory == null) {
            throw new IllegalArgumentException("storageRootDirectory cannot be null");
        }
        this.storageRootDirectory = storageRootDirectory;
    }

    public File storageRootDirectory() {
        return storageRootDirectory;
    }
}
