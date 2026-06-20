package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.core.BlueMatrixContainer;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;

public final class ConfigExtension implements BlueMatrixExtension {
    private ModuleConfigRegistry configRegistry;

    @Override
    public void apply(BlueMatrixContainer.Builder builder, BlueMatrixExtensionContext context) {
        this.configRegistry = new ModuleConfigRegistry(builder.getDataFolder(), new YamlConfigFileFormat());
        builder.repository(
                        "https://jitpack.io"
                )
                .extensionLibrary(
                        context.getName(),
                        BlueLibraryFactory.of("me.carleslc.Simple-YAML:Simple-Yaml:1.8.4")
                                .relocate("org.yaml", "io.fntlv.bluematrix.libs.yaml"),
                        "org.simpleyaml.configuration.file.YamlFile"
                )
                .extensionLibrary(
                        context.getName(),
                        "com.google.code.gson:gson:2.11.0",
                        "com.google.gson.Gson"
                )
                .parameterResolver(new ConfigContextResolver(configRegistry))
                .eventListener(new ConfigModuleListener(configRegistry));
    }

}
