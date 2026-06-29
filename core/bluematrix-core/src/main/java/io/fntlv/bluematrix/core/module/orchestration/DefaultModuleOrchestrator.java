package io.fntlv.bluematrix.core.module.orchestration;

import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.LifecycleManager;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrar;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationResult;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultModuleOrchestrator implements ModuleOrchestrator {
    private final ModuleStore moduleStore;
    private final ModuleRegistrar moduleRegistrar;
    private final LifecycleManager lifecycle;
    private boolean initialized;

    public DefaultModuleOrchestrator(ModuleStore moduleStore,
                                     ModuleRegistrar moduleRegistrar,
                                     LifecycleManager lifecycle) {
        if (moduleStore == null) {
            throw new IllegalArgumentException("moduleStore cannot be null");
        }
        if (moduleRegistrar == null) {
            throw new IllegalArgumentException("moduleRegistrar cannot be null");
        }
        if (lifecycle == null) {
            throw new IllegalArgumentException("lifecycle cannot be null");
        }
        this.moduleStore = moduleStore;
        this.moduleRegistrar = moduleRegistrar;
        this.lifecycle = lifecycle;
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }
        registerModules();
        initialized = true;
    }

    private void registerModules() {
        ModuleRegistrationResult result = moduleRegistrar.register();
        for (ModuleContext context : result.contexts()) {
            moduleStore.add(context);
        }
    }

    @Override
    public void loadModules() {
        for (ModuleContext moduleContext : moduleStore.all()) {
            lifecycle.loadModule(moduleContext);
        }
    }

    @Override
    public void enableModules() {
        for (ModuleContext moduleContext : moduleStore.all()) {
            lifecycle.enableModule(moduleContext);
        }
    }

    @Override
    public void disableModules() {
        List<ModuleContext> disableOrder = new ArrayList<>(moduleStore.all());
        Collections.reverse(disableOrder);
        for (ModuleContext moduleContext : disableOrder) {
            lifecycle.disableModule(moduleContext);
        }
    }
}
