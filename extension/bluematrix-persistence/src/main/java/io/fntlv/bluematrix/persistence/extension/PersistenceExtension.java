package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;

public final class PersistenceExtension implements BlueMatrixExtension {
    private static final String CAPABILITY_ID = "persistence";
    private static final String EVERY_DATABASE_CORE = "br.com.finalcraft.everydatabase:everydatabase-core:1.0.5";
    private static final String EVERY_DATABASE_MANAGER = "br.com.finalcraft.everydatabase:everydatabase-manager:1.0.5";
    private static final String HIKARI_CP = "com.zaxxer:HikariCP:4.0.3";
    private static final String HIKARI_PACKAGE = "com.zaxxer.hikari";
    private static final String RELOCATED_HIKARI_PACKAGE = "io.fntlv.bluematrix.persistence.libs.hikari";

    private static final String[] EVERY_DATABASE_RUNTIME_LIBRARIES = {
            "com.fasterxml.jackson.core:jackson-core:2.22.0",
            "com.fasterxml.jackson.core:jackson-annotations:2.22.0",
            "com.fasterxml.jackson.core:jackson-databind:2.22.0",
            "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.22.0",
            "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.0",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.0",
            "org.yaml:snakeyaml:2.6",
            "org.slf4j:slf4j-api:1.7.36",
            "com.h2database:h2:1.4.200",
            "org.mongodb:mongodb-driver-sync:5.8.0",
            "org.mongodb:mongodb-driver-core:5.8.0",
            "org.mongodb:bson:5.8.0",
            "org.mongodb:bson-record-codec:5.8.0",
            "com.mysql:mysql-connector-j:9.7.0",
            "org.postgresql:postgresql:42.7.12"
    };

    @Override
    public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        ModulePersistenceInitializer initializer = new ModulePersistenceInitializer(bootstrap.dataFolder());
        bootstrap.repository("https://maven.petrus.dev/public")
                .repository("https://repo.maven.apache.org/maven2")
                .extensionLibrary(
                        context.getName(),
                        BlueLibraryFactory.of(EVERY_DATABASE_CORE)
                                .relocate(HIKARI_PACKAGE, RELOCATED_HIKARI_PACKAGE),
                        "br.com.finalcraft.everydatabase.Storage"
                )
                .extensionLibrary(
                        context.getName(),
                        BlueLibraryFactory.of(EVERY_DATABASE_MANAGER),
                        "br.com.finalcraft.everydatabase.manager.CachingManager"
                )
                .extensionLibrary(
                        context.getName(),
                        BlueLibraryFactory.of(HIKARI_CP)
                                .relocate(HIKARI_PACKAGE, RELOCATED_HIKARI_PACKAGE)
                )
                .moduleCapability(ModuleCapability.<ModulePersistenceContext, ModulePersistenceState>builder(CAPABILITY_ID)
                        .contextType(ModulePersistenceContext.class)
                        .enabledWhen(candidate -> BlueStorageSourceProvider.class.isAssignableFrom(candidate.getModuleClass()))
                        .stateFactory(moduleId -> new ModulePersistenceState())
                        .contextFactory(ModulePersistenceContext::new)
                        .onEnablePre((binding, event) -> initializer.initialize(
                                event.getContext(),
                                binding.state().storage(),
                                binding.state().cacheSyncCoordinator()
                        ))
                        .onDisablePost((binding, event) -> close(binding.state()))
                        .onDisableFailed((binding, event) -> close(binding.state()))
                        .build());
        for (String library : EVERY_DATABASE_RUNTIME_LIBRARIES) {
            bootstrap.extensionLibrary(context.getName(), library);
        }
    }

    private void close(ModulePersistenceState state) {
        state.dataAccess().flushAllDirty().join();
        state.cacheSyncCoordinator().close();
        state.storage().close();
    }
}
