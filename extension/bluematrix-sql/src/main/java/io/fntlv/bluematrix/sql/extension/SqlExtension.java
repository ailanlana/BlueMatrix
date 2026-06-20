package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.BlueMatrixContainer;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;

public final class SqlExtension implements BlueMatrixExtension {
    @Override
    public void apply(BlueMatrixContainer.Builder builder, BlueMatrixExtensionContext context) {
        ModuleSqlRegistry sqlRegistry = new ModuleSqlRegistry();
        builder.repository("https://repo.carm.cc/repository/maven-public/")
                .extensionLibrary(
                        context.getName(),
                        "cc.carm.lib:easysql-api:0.4.7",
                        "cc.carm.lib.easysql.api.SQLManager"
                )
                .extensionLibrary(
                        context.getName(),
                        "cc.carm.lib:easysql-hikaricp:0.4.7",
                        "cc.carm.lib.easysql.EasySQL"
                )
                .extensionLibrary(
                        context.getName(),
                        "com.mysql:mysql-connector-j:9.4.0",
                        "com.mysql.cj.jdbc.Driver"
                )
                .parameterResolver(new SqlDatabaseResolver(sqlRegistry))
                .eventListener(new SqlModuleListener(sqlRegistry));
    }
}
