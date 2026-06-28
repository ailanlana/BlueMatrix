package io.fntlv.bluematrix.core.bootstrap;

import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.loader.library.BlueLibrary;

import java.io.File;

final class DefaultBlueMatrixExtensionBootstrap implements BlueMatrixExtensionBootstrap {
    private final BlueMatrixBootstrapPlan plan;

    DefaultBlueMatrixExtensionBootstrap(BlueMatrixBootstrapPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan cannot be null");
        }
        this.plan = plan;
    }

    @Override
    public File dataFolder() {
        return plan.dataFolder();
    }

    @Override
    public BlueMatrixExtensionBootstrap repository(String repositoryUrl) {
        plan.repository(repositoryUrl);
        return this;
    }

    @Override
    public BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, String coordinates) {
        plan.extensionLibrary(extensionName, coordinates);
        return this;
    }

    @Override
    public BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, BlueLibrary library) {
        plan.extensionLibrary(extensionName, library);
        return this;
    }

    @Override
    public BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, String coordinates, String presenceClass) {
        plan.extensionLibrary(extensionName, coordinates, presenceClass);
        return this;
    }

    @Override
    public BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, BlueLibrary library, String presenceClass) {
        plan.extensionLibrary(extensionName, library, presenceClass);
        return this;
    }

    @Override
    public BlueMatrixExtensionBootstrap parameterResolver(ModuleParameterResolver resolver) {
        plan.parameterResolver(resolver);
        return this;
    }

    @Override
    public BlueMatrixExtensionBootstrap eventListener(Object listener) {
        plan.eventListener(listener);
        return this;
    }
}
