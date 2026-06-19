package io.fntlv.bluematrix.core.extension;

import io.fntlv.bluematrix.core.BlueMatrixContainer;

public interface BlueMatrixExtension {
    void apply(BlueMatrixContainer.Builder builder, BlueMatrixExtensionContext context);

    default void launch(BlueMatrixContainer container, BlueMatrixExtensionContext context) {
    }
}
