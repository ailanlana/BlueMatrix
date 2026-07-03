package io.fntlv.bluematrix.persistence.core.storage.source;

import br.com.finalcraft.everydatabase.modules.sql.PoolTuning;
import br.com.finalcraft.everydatabase.modules.sql.SqlConfig;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorageSpec;

import java.time.Duration;

public interface BlueSqlStorageSource extends BlueStorageSource {
    default BlueSqlType getSqlType() {
        return BlueSqlType.SQL;
    }

    String getJdbcUrl();

    String getUsername();

    String getPassword();

    default int getPoolMinIdle() {
        return 2;
    }

    default int getPoolMaxSize() {
        return 10;
    }

    default long getPoolConnectTimeoutMillis() {
        return 30000L;
    }

    default long getPoolIdleTimeoutMillis() {
        return 600000L;
    }

    default long getPoolMaxLifetimeMillis() {
        return 1800000L;
    }

    @Override
    default BlueStorageSpec toSpec(BlueStorageSourceContext context) {
        SqlConfig config = getSqlConfig(context);
        BlueSqlType sqlType = getSqlType();
        if (sqlType == null) {
            throw new IllegalArgumentException("sqlType cannot be null");
        }
        switch (sqlType) {
            case SQL:
                return BlueStorageSpec.sql(config);
            case POSTGRESQL:
                return BlueStorageSpec.postgresql(config);
            case H2:
                return BlueStorageSpec.h2(config);
            default:
                throw new IllegalArgumentException("Unsupported SQL storage type: " + sqlType);
        }
    }

    default SqlConfig getSqlConfig(BlueStorageSourceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        PoolTuning pool = new PoolTuning(
                getPoolMinIdle(),
                getPoolMaxSize(),
                Duration.ofMillis(getPoolConnectTimeoutMillis()),
                Duration.ofMillis(getPoolIdleTimeoutMillis()),
                Duration.ofMillis(getPoolMaxLifetimeMillis())
        );
        return new SqlConfig(getJdbcUrl(), getUsername(), getPassword(), pool);
    }
}
