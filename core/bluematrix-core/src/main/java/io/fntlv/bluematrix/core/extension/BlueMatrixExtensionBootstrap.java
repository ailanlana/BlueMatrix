package io.fntlv.bluematrix.core.extension;

import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.loader.library.BlueLibrary;

import java.io.File;

public interface BlueMatrixExtensionBootstrap {
    File dataFolder();

    BlueMatrixExtensionBootstrap repository(String repositoryUrl);

    BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, String coordinates);

    BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, BlueLibrary library);

    BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, String coordinates, String presenceClass);

    BlueMatrixExtensionBootstrap extensionLibrary(String extensionName, BlueLibrary library, String presenceClass);

    BlueMatrixExtensionBootstrap parameterResolver(ModuleParameterResolver resolver);

    BlueMatrixExtensionBootstrap eventListener(Object listener);
}
