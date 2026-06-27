package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleDescriptor;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

public final class ModuleLibraryLoadListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(ModuleLibraryLoadListener.class);

    private final ModuleRuntimeLibraryLoader runtimeLibraryLoader;

    public ModuleLibraryLoadListener(BlueMatrixLibraryLoader libraryLoader) {
        if (libraryLoader == null) {
            throw new IllegalArgumentException("libraryLoader cannot be null");
        }
        this.runtimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);
    }

    @ModuleEventListener
    public void onRegisterPost(ModuleRegisterEvent.Post event) {
        ModuleContext context = event.getContext();
        ModuleDescriptor descriptor = context.getDescriptor();
        try {
            runtimeLibraryLoader.load(descriptor);
        } catch (RuntimeException e) {
            context.markError();
            LOGGER.error(String.format("Module library load failed: [module=%s]", descriptor.id()), e);
        }
    }
}
