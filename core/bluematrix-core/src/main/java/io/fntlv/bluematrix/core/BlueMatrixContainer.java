package io.fntlv.bluematrix.core;

import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionLoader;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.orchestration.DefaultModuleOrchestrator;
import io.fntlv.bluematrix.core.module.orchestration.ModuleOrchestrator;
import io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.registration.DefaultModuleRegistrar;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrar;
import io.fntlv.bluematrix.core.library.ModuleLibraryLoadListener;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleEventBusParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleRegistryParameterResolver;
import io.fntlv.bluematrix.core.module.registration.provider.JarModuleProvider;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.core.module.registration.provider.PackageModuleProvider;
import io.fntlv.bluematrix.core.module.registration.resolver.TopologyDependencyResolver;
import io.fntlv.bluematrix.core.module.storage.DefaultModuleRegistry;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class BlueMatrixContainer {
    @Getter
    private final ModuleRegistry registry;
    private final ModuleOrchestrator moduleOrchestrator;
    @Getter
    private final ModuleEventBus eventBus;
    @Getter
    private final ModuleParameterResolverRegistry parameterResolvers;
    @Getter
    private final ModuleInstanceFactory instanceFactory;

    private BlueMatrixContainer(Builder builder) {
        ModuleStore moduleStore = new ModuleStore();
        this.eventBus = new DefaultModuleEventBus();
        this.registry = new DefaultModuleRegistry(moduleStore, builder.dataFolder);
        this.parameterResolvers = ModuleParameterResolverRegistry.createDefault();
        this.parameterResolvers.register(new ModuleRegistryParameterResolver(registry));
        this.parameterResolvers.register(new ModuleEventBusParameterResolver(eventBus));
        this.instanceFactory = new DefaultModuleInstanceFactory(parameterResolvers);
        ModuleRegistrar moduleRegistrar = new DefaultModuleRegistrar(
                builder.moduleProviders,
                new TopologyDependencyResolver(),
                eventBus,
                instanceFactory
        );
        this.moduleOrchestrator = new DefaultModuleOrchestrator(moduleStore, moduleRegistrar, eventBus);
        registerListeners(builder.eventListeners);
        registerDefaultListeners(builder.libraryLoader);
        eventBus.publish(new BlueMatrixContainerEvent.Created(parameterResolvers, instanceFactory));
        this.moduleOrchestrator.initialize();
    }

    public static Builder builder(File dataFolder) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        return new Builder(dataFolder);
    }

    private void registerListeners(List<Object> eventListeners) {
        for (Object eventListener : eventListeners) {
            eventBus.registerListener(eventListener);
        }
    }

    private void registerDefaultListeners(BlueMatrixLibraryLoader libraryLoader) {
        eventBus.registerListener(new ModuleLibraryLoadListener(libraryLoader));
    }

    public void loadModules() {
        moduleOrchestrator.loadModules();
    }

    public void enableModules() {
        moduleOrchestrator.enableModules();
    }

    public void disableModules() {
        moduleOrchestrator.disableModules();
    }

    public static final class Builder {
        @Getter
        private final File dataFolder;
        private final List<ModuleProvider> moduleProviders = new ArrayList<>();
        private final List<Object> eventListeners = new ArrayList<>();
        private final BlueMatrixLibraryLoader libraryLoader;
        private boolean built;

        private Builder(File dataFolder) {
            this.dataFolder = dataFolder;
            this.libraryLoader = new BlueMatrixLibraryLoader(dataFolder, BlueMatrixContainer.class.getClassLoader());
        }

        public Builder packageScan(String packagePath) {
            if (packagePath == null || packagePath.trim().isEmpty()) {
                throw new IllegalArgumentException("packagePath cannot be blank");
            }
            moduleProviders.add(new PackageModuleProvider(packagePath));
            return this;
        }

        public Builder jarDirectory(File jarDirectory) {
            if (jarDirectory == null) {
                throw new IllegalArgumentException("jarDirectory cannot be null");
            }
            moduleProviders.add(new JarModuleProvider(jarDirectory));
            return this;
        }

        public Builder eventListener(Object listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener cannot be null");
            }
            eventListeners.add(listener);
            return this;
        }

        public Builder appLibrary(String coordinates) {
            return appLibrary(BlueLibraryFactory.of(coordinates));
        }

        public Builder appLibrary(BlueLibrary library) {
            libraryLoader.addAppLibrary(library);
            return this;
        }

        public Builder appLibrary(String coordinates, String presenceClass) {
            libraryLoader.addAppLibrary(coordinates, presenceClass);
            return this;
        }

        public Builder appLibrary(BlueLibrary library, String presenceClass) {
            libraryLoader.addAppLibrary(library, presenceClass);
            return this;
        }

        public Builder repository(String repositoryUrl) {
            libraryLoader.addRepository(repositoryUrl);
            return this;
        }

        public Builder extensionLibrary(String extensionName, String coordinates) {
            return extensionLibrary(extensionName, BlueLibraryFactory.of(coordinates));
        }

        public Builder extensionLibrary(String extensionName, BlueLibrary library) {
            libraryLoader.addExtensionLibrary(extensionName, library);
            return this;
        }

        public Builder extensionLibrary(String extensionName, String coordinates, String presenceClass) {
            libraryLoader.addExtensionLibrary(extensionName, coordinates, presenceClass);
            return this;
        }

        public Builder extensionLibrary(String extensionName, BlueLibrary library, String presenceClass) {
            libraryLoader.addExtensionLibrary(extensionName, library, presenceClass);
            return this;
        }

        public BlueMatrixContainer build() {
            if (built) {
                throw new BlueMatrixContainerException("BlueMatrixContainer.Builder cannot be reused");
            }
            built = true;
            BlueMatrixExtensionLoader extensionLoader = new BlueMatrixExtensionLoader().load();
            try {
                this.libraryLoader.loadCoreLibraries();
            } catch (RuntimeException e) {
                throw new BlueMatrixContainerException("Failed to load BlueMatrix runtime libraries", e);
            }
            extensionLoader.apply(this);
            try {
                this.libraryLoader.loadAppLibraries();
                this.libraryLoader.loadExtensionLibraries();
            } catch (RuntimeException e) {
                throw new BlueMatrixContainerException("Failed to load BlueMatrix runtime libraries", e);
            }
            if (moduleProviders.isEmpty()) {
                throw new BlueMatrixContainerException("At least one module provider is required");
            }
            BlueMatrixContainer blueMatrixContainer = new BlueMatrixContainer(this);
            extensionLoader.launch(blueMatrixContainer);
            return blueMatrixContainer;
        }
    }
}
