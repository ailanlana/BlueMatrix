package io.fntlv.bluematrix.sql.core;

import cc.carm.lib.easysql.EasySQL;
import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.hikari.HikariConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BlueDatabase {
    private volatile SQLManager sqlManager;

    public BlueDatabase() {
    }

    public BlueDatabase(SQLManager sqlManager) {
        if (sqlManager == null) {
            throw new IllegalArgumentException("sqlManager cannot be null");
        }
        this.sqlManager = sqlManager;
    }

    private BlueDatabase(Builder builder) {
        String jdbcUrl = buildJdbcUrl(builder.ip, builder.port, builder.database, builder.jdbcParams);
        HikariConfig config = createHikariConfig(
                builder.driverClassName,
                jdbcUrl,
                builder.username,
                builder.password,
                builder.maxPoolSize,
                builder.connectionTimeout,
                builder.idleTimeout,
                builder.dataSourceProperties
        );
        this.sqlManager = EasySQL.createManager(config);
    }

    public static Builder builder(BlueDatabaseSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null");
        }
        Builder builder = new Builder(
                source.getUsername(),
                source.getPassword(),
                source.getIp(),
                source.getPort(),
                source.getDatabase()
        );
        builder.driverClassName(source.getDriverClassName())
                .maxPoolSize(source.getMaxPoolSize())
                .connectionTimeout(source.getConnectionTimeout())
                .idleTimeout(source.getIdleTimeout());
        for (Map.Entry<String, String> entry : source.getJdbcParams().entrySet()) {
            builder.addJdbcParam(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : source.getDataSourceProperties().entrySet()) {
            builder.addDataSourceProperty(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    public void close() {
        SQLManager current = sqlManager;
        if (current != null) {
            EasySQL.shutdownManager(current);
        }
    }

    public boolean available() {
        return sqlManager != null;
    }

    public SQLManager sqlManager() {
        SQLManager current = sqlManager;
        if (current == null) {
            throw new IllegalStateException("BlueDatabase is not initialized");
        }
        return current;
    }

    public SQLManager getSqlManager() {
        return sqlManager();
    }

    public synchronized void initialize(BlueDatabaseSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null");
        }
        if (sqlManager != null) {
            throw new IllegalStateException("BlueDatabase is already initialized");
        }
        this.sqlManager = createSqlManager(source);
    }

    protected SQLManager createSqlManager(BlueDatabaseSource source) {
        return BlueDatabase.builder(source).build().sqlManager();
    }

    static String buildJdbcUrl(String ip, int port, String database, Map<String, String> params) {
        String baseUrl = String.format("jdbc:mysql://%s:%d/%s", ip, port, database);
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return baseUrl + "?" + query;
    }

    private static HikariConfig createHikariConfig(
            String driverClassName,
            String jdbcUrl,
            String username,
            String password,
            int maxPoolSize,
            long connectionTimeout,
            long idleTimeout,
            Map<String, String> dataSourceProperties
    ) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);

        configureDefaultMySqlProperties(config);

        if (dataSourceProperties != null) {
            for (Map.Entry<String, String> entry : dataSourceProperties.entrySet()) {
                config.addDataSourceProperty(entry.getKey(), entry.getValue());
            }
        }

        return config;
    }

    private static void configureDefaultMySqlProperties(HikariConfig config) {
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }

    public static class Builder {
        private final String username;
        private final String password;
        private final String ip;
        private final int port;
        private final String database;

        private String driverClassName = BlueDatabaseSource.DEFAULT_DRIVER_CLASS_NAME;
        private int maxPoolSize = 10;
        private long connectionTimeout = 30000L;
        private long idleTimeout = 600000L;
        private final Map<String, String> jdbcParams = new HashMap<>();
        private final Map<String, String> dataSourceProperties = new HashMap<>();

        public Builder(String username, String password, String ip, int port, String database) {
            this.username = requireNotBlank(username, "username");
            this.password = Objects.requireNonNull(password, "password cannot be null");
            this.ip = requireNotBlank(ip, "ip");
            this.port = requirePositive(port, "port");
            this.database = requireNotBlank(database, "database");
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = requireNotBlank(driverClassName, "driverClassName");
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = requirePositive(maxPoolSize, "maxPoolSize");
            return this;
        }

        public Builder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = requirePositive(connectionTimeout, "connectionTimeout");
            return this;
        }

        public Builder idleTimeout(long idleTimeout) {
            this.idleTimeout = requirePositive(idleTimeout, "idleTimeout");
            return this;
        }

        public Builder addJdbcParam(String key, String value) {
            jdbcParams.put(requireNotBlank(key, "key"), Objects.requireNonNull(value, "value cannot be null"));
            return this;
        }

        public Builder addDataSourceProperty(String key, String value) {
            dataSourceProperties.put(requireNotBlank(key, "key"), Objects.requireNonNull(value, "value cannot be null"));
            return this;
        }

        public BlueDatabase build() {
            return new BlueDatabase(this);
        }

        String getIp() {
            return ip;
        }

        int getPort() {
            return port;
        }

        String getDatabase() {
            return database;
        }

        String getDriverClassName() {
            return driverClassName;
        }

        int getMaxPoolSize() {
            return maxPoolSize;
        }

        long getConnectionTimeout() {
            return connectionTimeout;
        }

        long getIdleTimeout() {
            return idleTimeout;
        }

        Map<String, String> getJdbcParams() {
            return jdbcParams;
        }

        Map<String, String> getDataSourceProperties() {
            return dataSourceProperties;
        }

        private static String requireNotBlank(String value, String name) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(name + " cannot be blank");
            }
            return value;
        }

        private static int requirePositive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }

        private static long requirePositive(long value, String name) {
            if (value <= 0L) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }
    }
}
