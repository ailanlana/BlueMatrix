package io.fntlv.bluematrix.persistence.core.sources;

import io.fntlv.bluematrix.persistence.core.BlueStorageSpec;

public interface BlueStorageSource {
    BlueStorageSpec toSpec(BlueStorageSourceContext context);
}
