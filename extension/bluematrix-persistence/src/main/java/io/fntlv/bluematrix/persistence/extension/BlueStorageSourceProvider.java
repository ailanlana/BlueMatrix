package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.persistence.core.storage.source.BlueStorageSource;

public interface BlueStorageSourceProvider {
    BlueStorageSource getStorageSource();
}
