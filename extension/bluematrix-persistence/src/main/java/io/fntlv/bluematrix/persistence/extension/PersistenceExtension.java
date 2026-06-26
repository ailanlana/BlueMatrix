package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.BlueMatrixContainer;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;

public final class PersistenceExtension implements BlueMatrixExtension {
    private static final String EVERY_DATABASE_CORE = "br.com.finalcraft.everydatabase:everydatabase-core:1.0.4";
    private static final String HIKARI_CP = "com.zaxxer:HikariCP:4.0.3";
    private static final String HIKARI_PACKAGE = "com.zaxxer.hikari";
    private static final String RELOCATED_HIKARI_PACKAGE = "io.fntlv.bluematrix.persistence.libs.hikari";

    private static final String[] EVERY_DATABASE_RUNTIME_LIBRARIES = {
            "com.fasterxml.jackson.core:jackson-core:2.15.4",
            "com.fasterxml.jackson.core:jackson-annotations:2.15.4",
            "com.fasterxml.jackson.core:jackson-databind:2.15.4",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.4",
            "org.yaml:snakeyaml:2.1",
            "org.slf4j:slf4j-api:1.7.36",
            "com.h2database:h2:1.4.200",
            "org.mongodb:mongodb-driver-sync:4.11.2",
            "org.mongodb:mongodb-driver-core:4.11.2",
            "org.mongodb:bson:4.11.2",
            "org.mongodb:bson-record-codec:4.11.2",
            "com.mysql:mysql-connector-j:9.4.0",
            "org.postgresql:postgresql:42.7.7"
    };

    @Override
    public void apply(BlueMatrixContainer.Builder builder, BlueMatrixExtensionContext context) {
        ModulePersistenceRegistry persistenceRegistry = new ModulePersistenceRegistry(builder.getDataFolder());
        builder.repository("https://maven.petrus.dev/public")
                .repository("https://repo.maven.apache.org/maven2")
                .extensionLibrary(
                        context.getName(),
                        BlueLibraryFactory.of(EVERY_DATABASE_CORE)
                                .relocate(HIKARI_PACKAGE, RELOCATED_HIKARI_PACKAGE),
                        "br.com.finalcraft.everydatabase.Storage"
                )
                .extensionLibrary(
                        context.getName(),
                        BlueLibraryFactory.of(HIKARI_CP)
                                .relocate(HIKARI_PACKAGE, RELOCATED_HIKARI_PACKAGE)
                );
        for (String library : EVERY_DATABASE_RUNTIME_LIBRARIES) {
            builder.extensionLibrary(context.getName(), library);
        }
        builder.parameterResolver(new PersistenceStorageResolver(persistenceRegistry))
                .eventListener(new PersistenceModuleListener(persistenceRegistry));
    }
}
