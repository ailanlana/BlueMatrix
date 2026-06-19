package io.fntlv.bluematrix.persistence.core.sources;

import io.fntlv.bluematrix.persistence.core.BlueStorageSpec;

public interface BlueInMemoryStorageSource extends BlueStorageSource {
    @Override
    default BlueStorageSpec toSpec(BlueStorageSourceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        return BlueStorageSpec.inMemory();
    }
}
