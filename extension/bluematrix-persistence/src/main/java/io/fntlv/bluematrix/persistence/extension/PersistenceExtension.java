package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.BlueMatrixContainer;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;

public final class PersistenceExtension implements BlueMatrixExtension {
    @Override
    public void apply(BlueMatrixContainer.Builder builder, BlueMatrixExtensionContext context) {
        ModulePersistenceRegistry persistenceRegistry = new ModulePersistenceRegistry(builder.getDataFolder());
        builder.repository("https://maven.petrus.dev/public")
                .extensionLibrary(
                        context.getName(),
                        "br.com.finalcraft.everydatabase:everydatabase-standalone:1.0.1",
                        "br.com.finalcraft.everydatabase.Storage"
                )
                .extensionLibrary(
                        context.getName(),
                        "com.mysql:mysql-connector-j:9.4.0",
                        "com.mysql.cj.jdbc.Driver"
                )
                .parameterResolver(new PersistenceStorageResolver(persistenceRegistry))
                .eventListener(new PersistenceModuleListener(persistenceRegistry));
    }
}
