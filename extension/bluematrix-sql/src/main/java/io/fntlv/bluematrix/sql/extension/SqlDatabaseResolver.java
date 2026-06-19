package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolver;
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
    public boolean supports(Class<?> parameterType) {
        return BlueDatabase.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolve(Class<?> parameterType, ModuleCandidate candidate) {
        if (!BlueDatabaseSourceProvider.class.isAssignableFrom(candidate.getModuleClass())) {
            throw new IllegalStateException("BlueDatabase injection requires module to implement "
                    + "BlueDatabaseSourceProvider: " + candidate.getModuleInfo().id()
                    + " (" + candidate.getModuleClass().getName() + ")");
        }
        return sqlRegistry.getDatabase(candidate);
    }
}
