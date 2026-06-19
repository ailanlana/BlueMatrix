package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.persistence.core.sources.BlueStorageSource;

public interface BlueStorageSourceProvider {
    BlueStorageSource getStorageSource();
}
