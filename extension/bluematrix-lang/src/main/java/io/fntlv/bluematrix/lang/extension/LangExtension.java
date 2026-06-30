package io.fntlv.bluematrix.lang.extension;

import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;
import io.fntlv.bluematrix.lang.core.loader.BlueLangLoader;

public final class LangExtension implements BlueMatrixExtension {
    private final BlueLangLoader langLoader = new BlueLangLoader();

    public BlueLangLoader langLoader() {
        return langLoader;
    }

    @Override
    public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        bootstrap.eventListener(new LangModuleListener(bootstrap.dataFolder(), langLoader));
    }
}
