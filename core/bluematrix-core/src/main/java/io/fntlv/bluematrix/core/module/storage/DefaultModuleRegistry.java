package io.fntlv.bluematrix.core.module.storage;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.ModuleContext;

import java.io.File;
import java.util.Optional;

public class DefaultModuleRegistry implements ModuleRegistry {
    private final ModuleStore moduleStore;
    private final File dataFolder;

    public DefaultModuleRegistry(ModuleStore moduleStore, File dataFolder) {
        this.moduleStore = moduleStore;
        this.dataFolder = dataFolder;
    }

    @Override
    public <T extends Module> Optional<T> getModule(Class<T> clazz) {
        return moduleStore.findByClass(clazz)
                .map(moduleContext -> clazz.cast(moduleContext.getInstance()));
    }

    @Override
    public boolean isEnabled(String moduleID) {
        return moduleStore.findById(moduleID)
                .map(ModuleContext::isEnabled)
                .orElse(false);
    }

    @Override
    public File getPath(Module module) {
        return moduleStore.findByInstance(module)
                .map(moduleContext -> new File(dataFolder, moduleContext.getInfo().id()))
                .orElse(null);
    }
}
