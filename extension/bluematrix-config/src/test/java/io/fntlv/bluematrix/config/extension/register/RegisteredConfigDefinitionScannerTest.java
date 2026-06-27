package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegisteredConfigDefinitionScannerTest {

    @Test
    void scansRegisteredFieldDefinitions() {
        RegisteredConfigDefinitionScanner scanner = new RegisteredConfigDefinitionScanner();

        List<RegisteredConfigField> fields = scanner.scan(DefinitionConfig.class, "definition");

        assertEquals(2, fields.size());
        assertEquals("definition.message", fields.get(0).path());
        assertEquals("hello", fields.get(0).defaultValue());
        assertEquals("message comment", fields.get(0).comment());
        assertEquals("definition.values", fields.get(1).path());
        assertEquals(Integer.class, fields.get(1).listElementType());
    }

    @Test
    void rejectsListFieldWithoutGenericType() {
        RegisteredConfigDefinitionScanner scanner = new RegisteredConfigDefinitionScanner();

        ConfigInjectionException exception = assertThrows(ConfigInjectionException.class,
                () -> scanner.scan(RawListConfig.class, "definition"));

        assertEquals(ConfigDefinitionException.class, exception.getCause().getClass());
    }

    private static class DefinitionConfig {
        @BlueConfig.Field(path = "message", defaultValue = "hello", comment = "message comment")
        private String message;

        @BlueConfig.Field(path = "values", defaultValue = {"1", "2"})
        private List<Integer> values;
    }

    @SuppressWarnings("rawtypes")
    private static class RawListConfig {
        @BlueConfig.Field(path = "values", defaultValue = {})
        private List values;
    }
}
