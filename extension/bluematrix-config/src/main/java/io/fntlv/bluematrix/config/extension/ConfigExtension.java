package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;
import io.fntlv.bluematrix.core.module.ModuleConditionOutcome;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityBinding;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

public final class ConfigExtension implements BlueMatrixExtension {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(ConfigExtension.class);
    private static final String CAPABILITY_ID = "config";

    @Override
    public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        ModuleConfigInitializer initializer = new ModuleConfigInitializer(
                bootstrap.dataFolder(),
                new YamlConfigFileFormat(),
                new ConfigRegisterProcessor()
        );
        bootstrap.repository(
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
                .moduleCapability(ModuleCapability.<ModuleConfigContext, ModuleConfigState>builder(CAPABILITY_ID)
                        .contextType(ModuleConfigContext.class)
                        .stateFactory(initializer::createState)
                        .contextFactory(ModuleConfigContext::new)
                        .onLoadPre((binding, event) -> {
                            try {
                                initializer.initialize(event.getContext(), binding.state());
                            } catch (RuntimeException e) {
                                event.error(CAPABILITY_ID, "Module configuration failed", e);
                            }
                        })
                        .onEnablePre((binding, event) -> {
                            if (!binding.state().moduleEnabled()) {
                                event.cancel(ModuleConditionOutcome.noMatch(
                                        CAPABILITY_ID,
                                        "Module disabled by configuration"
                                ));
                            }
                        })
                        .onDisablePost((binding, event) -> saveConfig(initializer, binding, event))
                        .onDisableFailed((binding, event) -> saveConfig(initializer, binding, event))
                        .build());
    }

    private static void saveConfig(ModuleConfigInitializer initializer,
                                   ModuleCapabilityBinding<ModuleConfigContext, ModuleConfigState> binding,
                                   ModuleDisableEvent event) {
        try {
            initializer.save(event.getContext(), binding.state());
        } catch (RuntimeException e) {
            LOGGER.error(String.format(
                    "Module configuration save failed during disable: [module=%s]",
                    event.getContext().id()
            ), e);
        }
    }
}
