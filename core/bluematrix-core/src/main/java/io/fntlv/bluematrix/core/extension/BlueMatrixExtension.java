package io.fntlv.bluematrix.core.extension;

import io.fntlv.bluematrix.core.BlueMatrixContainer;

public interface BlueMatrixExtension {
    void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context);

    default void launch(BlueMatrixContainer container, BlueMatrixExtensionContext context) {
    }
}
