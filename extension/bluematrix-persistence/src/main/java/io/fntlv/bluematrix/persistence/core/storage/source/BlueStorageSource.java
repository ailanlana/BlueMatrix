package io.fntlv.bluematrix.persistence.core.storage.source;

import io.fntlv.bluematrix.persistence.core.storage.BlueStorageSpec;

public interface BlueStorageSource {
    BlueStorageSpec toSpec(BlueStorageSourceContext context);
}
