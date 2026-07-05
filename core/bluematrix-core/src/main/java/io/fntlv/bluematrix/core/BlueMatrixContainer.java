package io.fntlv.bluematrix.core;

import io.fntlv.bluematrix.core.bootstrap.BlueMatrixBootstrap;
import io.fntlv.bluematrix.core.bootstrap.BlueMatrixBootstrapPlan;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionLoader;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.orchestration.ModuleOrchestrator;
import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoader;
import io.fntlv.bluematrix.core.module.registration.provider.JarModuleProvider;
import io.fntlv.bluematrix.core.module.registration.provider.PackageModuleProvider;
import io.fntlv.bluematrix.loader.BlueClassLoaderSupport;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;
import lombok.Getter;

import java.io.File;
import java.util.Optional;

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
    private final BlueMatrixExtensionLoader extensionLoader;

    private BlueMatrixContainer(ModuleRegistry registry,
                                ModuleOrchestrator moduleOrchestrator,
                                ModuleEventBus eventBus,
                                ModuleParameterResolverRegistry parameterResolvers,
                                ModuleInstanceFactory instanceFactory,
                                BlueMatrixExtensionLoader extensionLoader) {
        this.registry = registry;
        this.moduleOrchestrator = moduleOrchestrator;
        this.eventBus = eventBus;
        this.parameterResolvers = parameterResolvers;
        this.instanceFactory = instanceFactory;
        this.extensionLoader = extensionLoader;
    }

    public static BlueMatrixContainer create(ModuleRegistry registry,
                                             ModuleOrchestrator moduleOrchestrator,
                                             ModuleEventBus eventBus,
                                             ModuleParameterResolverRegistry parameterResolvers,
                                             ModuleInstanceFactory instanceFactory,
                                             BlueMatrixExtensionLoader extensionLoader) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        if (moduleOrchestrator == null) {
            throw new IllegalArgumentException("moduleOrchestrator cannot be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("eventBus cannot be null");
        }
        if (parameterResolvers == null) {
            throw new IllegalArgumentException("parameterResolvers cannot be null");
        }
        if (instanceFactory == null) {
            throw new IllegalArgumentException("instanceFactory cannot be null");
        }
        if (extensionLoader == null) {
            throw new IllegalArgumentException("extensionLoader cannot be null");
        }
        return new BlueMatrixContainer(
                registry,
                moduleOrchestrator,
                eventBus,
                parameterResolvers,
                instanceFactory,
                extensionLoader
        );
    }

    public static Builder builder(File dataFolder) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        return new Builder(dataFolder, defaultClassLoader());
    }

    public static Builder builder(File dataFolder, ClassLoader classLoader) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null");
        }
        return new Builder(dataFolder, classLoader);
    }

    private static ClassLoader defaultClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = BlueMatrixContainer.class.getClassLoader();
        }
        return classLoader;
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

    public <T extends BlueMatrixExtension> Optional<T> getExtension(Class<T> clazz) {
        return extensionLoader.getExtension(clazz);
    }

    public static final class Builder implements BlueMatrixExtensionBootstrap {
        private final ClassLoader classLoader;
        private final BlueMatrixBootstrapPlan plan;
        private final ModuleRuntimeLibraryLoader moduleRuntimeLibraryLoader;
        private boolean built;

        private Builder(File dataFolder, ClassLoader classLoader) {
            this.classLoader = BlueClassLoaderSupport.ensureUrlClassLoader(classLoader);
            BlueMatrixLibraryLoader libraryLoader = new BlueMatrixLibraryLoader(dataFolder, this.classLoader);
            this.plan = new BlueMatrixBootstrapPlan(dataFolder, this.classLoader, libraryLoader);
            this.moduleRuntimeLibraryLoader = new ModuleRuntimeLibraryLoader(libraryLoader);
        }

        public File getDataFolder() {
            return dataFolder();
        }

        @Override
        public File dataFolder() {
            return plan.dataFolder();
        }

        public Builder packageScan(String packagePath) {
            if (packagePath == null || packagePath.trim().isEmpty()) {
                throw new IllegalArgumentException("packagePath cannot be blank");
            }
            plan.addModuleProvider(new PackageModuleProvider(packagePath, moduleRuntimeLibraryLoader));
            return this;
        }

        public Builder jarDirectory(File jarDirectory) {
            if (jarDirectory == null) {
                throw new IllegalArgumentException("jarDirectory cannot be null");
            }
            plan.addModuleProvider(new JarModuleProvider(jarDirectory, classLoader, moduleRuntimeLibraryLoader));
            return this;
        }

        @Override
        public Builder eventListener(Object listener) {
            plan.eventListener(listener);
            return this;
        }

        @Override
        public Builder parameterResolver(ModuleParameterResolver resolver) {
            plan.parameterResolver(resolver);
            return this;
        }

        @Override
        public Builder moduleCapability(ModuleCapability<?, ?> capability) {
            plan.moduleCapability(capability);
            return this;
        }

        public Builder appLibrary(String coordinates) {
            return appLibrary(BlueLibraryFactory.of(coordinates));
        }

        public Builder appLibrary(BlueLibrary library) {
            plan.libraryLoader().addAppLibrary(library);
            return this;
        }

        public Builder appLibrary(String coordinates, String presenceClass) {
            plan.libraryLoader().addAppLibrary(coordinates, presenceClass);
            return this;
        }

        public Builder appLibrary(BlueLibrary library, String presenceClass) {
            plan.libraryLoader().addAppLibrary(library, presenceClass);
            return this;
        }

        @Override
        public Builder repository(String repositoryUrl) {
            plan.repository(repositoryUrl);
            return this;
        }

        @Override
        public Builder extensionLibrary(String extensionName, String coordinates) {
            return extensionLibrary(extensionName, BlueLibraryFactory.of(coordinates));
        }

        @Override
        public Builder extensionLibrary(String extensionName, BlueLibrary library) {
            plan.extensionLibrary(extensionName, library);
            return this;
        }

        @Override
        public Builder extensionLibrary(String extensionName, String coordinates, String presenceClass) {
            plan.extensionLibrary(extensionName, coordinates, presenceClass);
            return this;
        }

        @Override
        public Builder extensionLibrary(String extensionName, BlueLibrary library, String presenceClass) {
            plan.extensionLibrary(extensionName, library, presenceClass);
            return this;
        }

        public BlueMatrixContainer build() {
            if (built) {
                throw new BlueMatrixContainerException("BlueMatrixContainer.Builder cannot be reused");
            }
            built = true;
            return new BlueMatrixBootstrap().start(plan);
        }
    }
}
