package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.sql.core.BlueDatabase;

public class SqlDatabaseResolver implements ModuleParameterResolver {
    private final ModuleSqlRegistry sqlRegistry;

    public SqlDatabaseResolver(ModuleSqlRegistry sqlRegistry) {
        if (sqlRegistry == null) {
            throw new IllegalArgumentException("sqlRegistry cannot be null");
        }
        this.sqlRegistry = sqlRegistry;
    }

    @Override
    public boolean supports(Class<?> parameterType, InjectContext context) {
        return context instanceof ModuleInjectionContext
                && BlueDatabase.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolve(Class<?> parameterType, InjectContext context) {
        if (!BlueDatabaseSourceProvider.class.isAssignableFrom(context.getModuleClass())) {
            throw new IllegalStateException("BlueDatabase injection requires module to implement "
                    + "BlueDatabaseSourceProvider: " + context.getModuleInfo().id()
                    + " (" + context.getModuleClass().getName() + ")");
        }
        return sqlRegistry.getDatabase(context.getModuleInfo().id());
    }
}
