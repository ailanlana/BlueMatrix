package io.fntlv.bluematrix.sql.core;

import java.util.Collections;
import java.util.Map;

public interface BlueDatabaseSource {
    String DEFAULT_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    default String getDriverClassName() {
        return DEFAULT_DRIVER_CLASS_NAME;
    }

    String getIp();

    int getPort();

    String getDatabase();

    String getUsername();

    String getPassword();

    default int getMaxPoolSize() {
        return 10;
    }

    default long getConnectionTimeout() {
        return 30000L;
    }

    default long getIdleTimeout() {
        return 600000L;
    }

    default Map<String, String> getJdbcParams() {
        return Collections.emptyMap();
    }

    default Map<String, String> getDataSourceProperties() {
        return Collections.emptyMap();
    }
}
