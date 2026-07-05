package io.fntlv.bluematrix.core.bootstrap;

import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.core.module.registration.provider.ModuleProvider;
import io.fntlv.bluematrix.loader.library.BlueLibrary;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlueMatrixBootstrapPlan {
    private final File dataFolder;
    private final ClassLoader classLoader;
    private final BlueMatrixLibraryLoader libraryLoader;
    private final List<ModuleProvider> moduleProviders = new ArrayList<>();
    private final List<Object> eventListeners = new ArrayList<>();
    private final List<ModuleParameterResolver> parameterResolvers = new ArrayList<>();
    private final List<ModuleCapability<?, ?>> moduleCapabilities = new ArrayList<>();

    public BlueMatrixBootstrapPlan(File dataFolder, ClassLoader classLoader, BlueMatrixLibraryLoader libraryLoader) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null");
        }
        if (libraryLoader == null) {
            throw new IllegalArgumentException("libraryLoader cannot be null");
        }
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
        this.libraryLoader = libraryLoader;
    }

    public File dataFolder() {
        return dataFolder;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public BlueMatrixLibraryLoader libraryLoader() {
        return libraryLoader;
    }

    public void addModuleProvider(ModuleProvider moduleProvider) {
        if (moduleProvider == null) {
            throw new IllegalArgumentException("moduleProvider cannot be null");
        }
        moduleProviders.add(moduleProvider);
    }

    public List<ModuleProvider> moduleProviders() {
        return Collections.unmodifiableList(moduleProviders);
    }

    public List<Object> eventListeners() {
        return Collections.unmodifiableList(eventListeners);
    }

    public List<ModuleParameterResolver> parameterResolvers() {
        return Collections.unmodifiableList(parameterResolvers);
    }

    public List<ModuleCapability<?, ?>> moduleCapabilities() {
        return Collections.unmodifiableList(moduleCapabilities);
    }

    public BlueMatrixBootstrapPlan repository(String repositoryUrl) {
        libraryLoader.addRepository(repositoryUrl);
        return this;
    }

    public BlueMatrixBootstrapPlan extensionLibrary(String extensionName, String coordinates) {
        libraryLoader.addExtensionLibrary(extensionName, coordinates);
        return this;
    }

    public BlueMatrixBootstrapPlan extensionLibrary(String extensionName, BlueLibrary library) {
        libraryLoader.addExtensionLibrary(extensionName, library);
        return this;
    }

    public BlueMatrixBootstrapPlan extensionLibrary(String extensionName, String coordinates, String presenceClass) {
        libraryLoader.addExtensionLibrary(extensionName, coordinates, presenceClass);
        return this;
    }

    public BlueMatrixBootstrapPlan extensionLibrary(String extensionName, BlueLibrary library, String presenceClass) {
        libraryLoader.addExtensionLibrary(extensionName, library, presenceClass);
        return this;
    }

    public BlueMatrixBootstrapPlan parameterResolver(ModuleParameterResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("resolver cannot be null");
        }
        parameterResolvers.add(resolver);
        return this;
    }

    public BlueMatrixBootstrapPlan eventListener(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        eventListeners.add(listener);
        return this;
    }

    public BlueMatrixBootstrapPlan moduleCapability(ModuleCapability<?, ?> capability) {
        if (capability == null) {
            throw new IllegalArgumentException("capability cannot be null");
        }
        moduleCapabilities.add(capability);
        return this;
    }
}
