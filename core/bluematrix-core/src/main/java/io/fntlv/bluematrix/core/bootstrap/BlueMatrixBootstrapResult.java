package io.fntlv.bluematrix.core.bootstrap;

import io.fntlv.bluematrix.core.BlueMatrixContainer;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionLoader;

public final class BlueMatrixBootstrapResult {
    private final BlueMatrixContainerRuntime runtime;
    private final BlueMatrixExtensionLoader extensionLoader;

    BlueMatrixBootstrapResult(BlueMatrixContainerRuntime runtime, BlueMatrixExtensionLoader extensionLoader) {
        this.runtime = runtime;
        this.extensionLoader = extensionLoader;
    }

    public BlueMatrixContainerRuntime runtime() {
        return runtime;
    }

    public void launchExtensions(BlueMatrixContainer container) {
        extensionLoader.launch(container);
    }
}
