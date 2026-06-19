package io.fntlv.bluematrix.logging.support;

import io.fntlv.bluematrix.logging.BlueLogLevel;
import io.fntlv.bluematrix.logging.backend.BlueLogBackend;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultBlueLoggerTest {

    @Test
    void debugDoesNotLogWhenDisabled() {
        RecordingBackend backend = new RecordingBackend();
        DefaultBlueLogger logger = logger(backend);

        logger.debug("hidden");

        assertEquals(0, backend.logCount);
    }

    @Test
    void formatsPlaceholderArguments() {
        RecordingBackend backend = new RecordingBackend();
        DefaultBlueLogger logger = logger(backend);

        logger.info("Hello {}, {}", "Blue", "Matrix");

        assertEquals(BlueLogLevel.INFO, backend.level);
        assertEquals("[INFO] Hello Blue, Matrix", backend.message);
    }

    @Test
    void passesTrailingThrowableToBackend() {
        RecordingBackend backend = new RecordingBackend();
        DefaultBlueLogger logger = logger(backend);
        IllegalStateException failure = new IllegalStateException("boom");

        logger.error("Failed to load {}", "module", failure);

        assertEquals(BlueLogLevel.ERROR, backend.level);
        assertEquals("[ERROR] Failed to load module", backend.message);
        assertSame(failure, backend.throwable);
    }

    @Test
    void passesWarnLevelToBackend() {
        RecordingBackend backend = new RecordingBackend();
        DefaultBlueLogger logger = logger(backend);

        logger.warn("Careful");

        assertEquals(BlueLogLevel.WARN, backend.level);
        assertEquals("[WARN] Careful", backend.message);
    }

    @Test
    void outputDoesNotContainAnsiColorCodes() {
        RecordingBackend backend = new RecordingBackend();
        DefaultBlueLogger logger = logger(backend);

        logger.info("Plain");

        assertEquals("[INFO] Plain", backend.message);
    }

    private static DefaultBlueLogger logger(RecordingBackend backend) {
        return new DefaultBlueLogger(backend);
    }

    private static class RecordingBackend implements BlueLogBackend {
        private int logCount;
        private BlueLogLevel level;
        private String message;
        private Throwable throwable;

        @Override
        public boolean isEnabled(BlueLogLevel level) {
            return true;
        }

        @Override
        public void log(BlueLogLevel level, String message) {
            this.logCount++;
            this.level = level;
            this.message = message;
        }

        @Override
        public void log(BlueLogLevel level, String message, Throwable throwable) {
            this.logCount++;
            this.level = level;
            this.message = message;
            this.throwable = throwable;
        }
    }
}
