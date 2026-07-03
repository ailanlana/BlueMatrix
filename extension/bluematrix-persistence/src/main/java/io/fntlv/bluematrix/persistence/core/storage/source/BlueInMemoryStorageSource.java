package io.fntlv.bluematrix.persistence.core.storage.source;

import io.fntlv.bluematrix.persistence.core.storage.BlueStorageSpec;

public interface BlueInMemoryStorageSource extends BlueStorageSource {
    @Override
    default BlueStorageSpec toSpec(BlueStorageSourceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        return BlueStorageSpec.inMemory();
    }
}
