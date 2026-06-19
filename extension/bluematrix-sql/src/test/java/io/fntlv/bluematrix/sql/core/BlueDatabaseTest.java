package io.fntlv.bluematrix.sql.core;

import org.junit.jupiter.api.Test;

import cc.carm.lib.easysql.api.SQLManager;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueDatabaseTest {

    @Test
    void buildsJdbcUrlWithoutParams() {
        assertEquals(
                "jdbc:mysql://127.0.0.1:3306/minecraft",
                BlueDatabase.buildJdbcUrl("127.0.0.1", 3306, "minecraft", Collections.emptyMap())
        );
    }

    @Test
    void buildsJdbcUrlWithParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("useSSL", "false");
        params.put("characterEncoding", "utf8");

        assertEquals(
                "jdbc:mysql://localhost:3306/demo?useSSL=false&characterEncoding=utf8",
                BlueDatabase.buildJdbcUrl("localhost", 3306, "demo", params)
        );
    }

    @Test
    void builderRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new BlueDatabase.Builder("root", "", "", 3306, "demo"));
        assertThrows(IllegalArgumentException.class, () -> new BlueDatabase.Builder("root", "", "localhost", 0, "demo"));
        assertThrows(IllegalArgumentException.class, () -> new BlueDatabase.Builder("root", "", "localhost", 3306, ""));
        assertThrows(IllegalArgumentException.class, () -> new BlueDatabase.Builder("root", "", "localhost", 3306, "demo").driverClassName(""));
        assertThrows(IllegalArgumentException.class, () -> new BlueDatabase.Builder("root", "", "localhost", 3306, "demo").maxPoolSize(0));
        assertThrows(IllegalArgumentException.class, () -> new BlueDatabase.Builder("root", "", "localhost", 3306, "demo").connectionTimeout(0));
        assertThrows(IllegalArgumentException.class, () -> new BlueDatabase.Builder("root", "", "localhost", 3306, "demo").idleTimeout(0));
    }

    @Test
    void holderIsUnavailableBeforeInitialize() {
        BlueDatabase database = new BlueDatabase();

        assertFalse(database.available());
        assertThrows(IllegalStateException.class, database::sqlManager);
    }

    @Test
    void initializeSetsSqlManagerAndRejectsRepeatedInitialize() {
        SQLManager manager = fakeSqlManager();
        TestBlueDatabase database = new TestBlueDatabase(manager);

        database.initialize(new DefaultDriverSource());

        assertTrue(database.available());
        assertSame(manager, database.sqlManager());
        assertThrows(IllegalStateException.class, () -> database.initialize(new DefaultDriverSource()));
    }

    @Test
    void closeBeforeInitializeDoesNothing() {
        BlueDatabase database = new BlueDatabase();

        database.close();

        assertFalse(database.available());
    }

    @Test
    void sourceDefaultsToMysqlDriver() {
        assertEquals("com.mysql.cj.jdbc.Driver", new DefaultDriverSource().getDriverClassName());
    }

    @Test
    void builderCopiesSourceValues() {
        RecordingSource source = new RecordingSource();

        BlueDatabase.Builder builder = BlueDatabase.builder(source);

        assertEquals(
                "jdbc:mysql://10.0.0.2:3307/game?useSSL=false",
                BlueDatabase.buildJdbcUrl(builder.getIp(), builder.getPort(), builder.getDatabase(), builder.getJdbcParams())
        );
        assertEquals(5, builder.getMaxPoolSize());
        assertEquals(1000L, builder.getConnectionTimeout());
        assertEquals(2000L, builder.getIdleTimeout());
        assertEquals("org.example.Driver", builder.getDriverClassName());
        assertEquals("true", builder.getDataSourceProperties().get("cachePrepStmts"));
    }

    private static final class RecordingSource implements BlueDatabaseSource {
        @Override
        public String getIp() {
            return "10.0.0.2";
        }

        @Override
        public int getPort() {
            return 3307;
        }

        @Override
        public String getDatabase() {
            return "game";
        }

        @Override
        public String getUsername() {
            return "root";
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public String getDriverClassName() {
            return "org.example.Driver";
        }

        @Override
        public int getMaxPoolSize() {
            return 5;
        }

        @Override
        public long getConnectionTimeout() {
            return 1000L;
        }

        @Override
        public long getIdleTimeout() {
            return 2000L;
        }

        @Override
        public Map<String, String> getJdbcParams() {
            Map<String, String> params = new HashMap<>();
            params.put("useSSL", "false");
            return params;
        }

        @Override
        public Map<String, String> getDataSourceProperties() {
            Map<String, String> properties = new HashMap<>();
            properties.put("cachePrepStmts", "true");
            return properties;
        }
    }

    private static final class DefaultDriverSource implements BlueDatabaseSource {
        @Override
        public String getIp() {
            return "localhost";
        }

        @Override
        public int getPort() {
            return 3306;
        }

        @Override
        public String getDatabase() {
            return "demo";
        }

        @Override
        public String getUsername() {
            return "root";
        }

        @Override
        public String getPassword() {
            return "";
        }
    }

    private static final class TestBlueDatabase extends BlueDatabase {
        private final SQLManager manager;

        private TestBlueDatabase(SQLManager manager) {
            this.manager = manager;
        }

        @Override
        protected SQLManager createSqlManager(BlueDatabaseSource source) {
            return manager;
        }
    }

    private static SQLManager fakeSqlManager() {
        return (SQLManager) Proxy.newProxyInstance(
                SQLManager.class.getClassLoader(),
                new Class<?>[]{SQLManager.class},
                (proxy, method, args) -> null
        );
    }
}
