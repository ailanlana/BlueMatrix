package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

public final class ModuleLibraryLoadListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(ModuleLibraryLoadListener.class);

    private final BlueMatrixLibraryLoader libraryLoader;

    public ModuleLibraryLoadListener(BlueMatrixLibraryLoader libraryLoader) {
        if (libraryLoader == null) {
            throw new IllegalArgumentException("libraryLoader cannot be null");
        }
        this.libraryLoader = libraryLoader;
    }

    @ModuleEventListener
    public void onRegisterPost(ModuleRegisterEvent.Post event) {
        ModuleContext context = event.getContext();
        ModuleInfo info = context.getInfo();
        String[] libraries = info.libraries();
        if (libraries.length == 0) {
            return;
        }

        try {
            for (String repository : info.repositories()) {
                libraryLoader.addModuleRepository(info.id(), repository);
            }
            for (String library : libraries) {
                libraryLoader.addModuleLibrary(info.id(), library);
            }
            libraryLoader.loadModuleLibraries(info.id());
        } catch (RuntimeException e) {
            context.markError();
            LOGGER.error(String.format("Module library load failed: [module=%s]", info.id()), e);
        }
    }
}
