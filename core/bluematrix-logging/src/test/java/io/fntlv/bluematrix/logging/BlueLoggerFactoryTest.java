package io.fntlv.bluematrix.logging;

import io.fntlv.bluematrix.logging.backend.BlueLogBackend;
import io.fntlv.bluematrix.logging.backend.BlueLogBackendProvider;
import io.fntlv.bluematrix.logging.support.DefaultBlueLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class BlueLoggerFactoryTest {
    private final BlueLogBackendProvider previousProvider = BlueLoggerFactory.getBackendProvider();
    private final boolean previousDebugEnabled = BlueLoggerFactory.isDebugEnabled();

    @AfterEach
    void resetLoggerFactory() {
        BlueLoggerFactory.setBackendProvider(previousProvider);
        BlueLoggerFactory.setDebugEnabled(previousDebugEnabled);
    }

    @Test
    void sameNameReturnsSameLogger() {
        BlueLogger first = BlueLoggerFactory.getLogger("test.same");
        BlueLogger second = BlueLoggerFactory.getLogger("test.same");

        assertSame(first, second);
    }

    @Test
    void differentNamesReturnDifferentLoggers() {
        BlueLogger first = BlueLoggerFactory.getLogger("test.first");
        BlueLogger second = BlueLoggerFactory.getLogger("test.second");

        assertNotSame(first, second);
    }

    @Test
    void classLoggerUsesClassName() {
        DefaultBlueLogger logger = (DefaultBlueLogger) BlueLoggerFactory.getLogger(BlueLoggerFactoryTest.class);

        assertEquals(BlueLoggerFactoryTest.class.getName(), logger.getName());
    }

    @Test
    void providerIsUsedForNewAndExistingLoggers() {
        RecordingBackendProvider firstProvider = new RecordingBackendProvider("first");
        RecordingBackendProvider secondProvider = new RecordingBackendProvider("second");
        DefaultBlueLogger logger = (DefaultBlueLogger) BlueLoggerFactory.getLogger("test.provider");

        BlueLoggerFactory.setBackendProvider(firstProvider);
        BlueLoggerFactory.getLogger("test.provider.new");
        assertEquals("first:test.provider", ((RecordingBackend) logger.getBackend()).id);

        BlueLoggerFactory.setBackendProvider(secondProvider);
        assertEquals("second:test.provider", ((RecordingBackend) logger.getBackend()).id);
    }

    @Test
    void debugSettingAppliesToExistingAndNewLoggers() {
        DefaultBlueLogger existing = (DefaultBlueLogger) BlueLoggerFactory.getLogger("test.settings.existing");

        BlueLoggerFactory.setDebugEnabled(true);
        DefaultBlueLogger createdAfterSettings = (DefaultBlueLogger) BlueLoggerFactory.getLogger("test.settings.new");

        assertEquals(true, existing.isDebugEnabled());
        assertEquals(true, createdAfterSettings.isDebugEnabled());
    }

    private static class RecordingBackendProvider implements BlueLogBackendProvider {
        private final String prefix;

        private RecordingBackendProvider(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public BlueLogBackend getBackend(String name) {
            return new RecordingBackend(prefix + ":" + name);
        }
    }

    private static class RecordingBackend implements BlueLogBackend {
        private final String id;

        private RecordingBackend(String id) {
            this.id = id;
        }

        @Override
        public boolean isEnabled(BlueLogLevel level) {
            return true;
        }

        @Override
        public void log(BlueLogLevel level, String message) {
        }

        @Override
        public void log(BlueLogLevel level, String message, Throwable throwable) {
        }
    }
}
