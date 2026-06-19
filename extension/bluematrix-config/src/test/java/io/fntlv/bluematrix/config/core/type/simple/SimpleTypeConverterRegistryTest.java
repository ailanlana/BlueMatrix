package io.fntlv.bluematrix.config.core.type.simple;

import io.fntlv.bluematrix.config.core.type.exception.ConfigValueConvertException;
import io.fntlv.bluematrix.config.core.type.simple.converters.NumberSimpleTypeConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleTypeConverterRegistryTest {

    private final SimpleTypeConverterRegistry converters = new SimpleTypeConverterRegistry();

    @Test
    void convertsStringValues() {
        assertEquals("3", converters.convert(3, String.class, "value"));
    }

    @Test
    void convertsBooleanValues() {
        assertEquals(true, converters.convert("true", Boolean.class, "enabled"));
        assertEquals(false, converters.convert("false", Boolean.class, "enabled"));
        assertEquals(true, converters.convert("1", Boolean.class, "enabled"));
        assertEquals(false, converters.convert("0", Boolean.class, "enabled"));
        assertEquals(true, converters.convert("yes", Boolean.class, "enabled"));
        assertEquals(false, converters.convert("no", Boolean.class, "enabled"));
    }

    @Test
    void convertsNumberValues() {
        assertEquals(3, converters.convert("3", Integer.class, "amount"));
        assertEquals(3L, converters.convert("3", Long.class, "amount"));
        assertEquals(1.5D, converters.convert("1.5", Double.class, "amount"));
        assertEquals(1.5F, converters.convert("1.5", Float.class, "amount"));
        assertEquals((short) 3, converters.convert("3", Short.class, "amount"));
        assertEquals((byte) 3, converters.convert("3", Byte.class, "amount"));
        assertEquals(3, converters.convert(3.0D, Integer.class, "amount"));
        assertEquals(3L, converters.convert(3, Long.class, "amount"));
    }

    @Test
    void convertsCharacterValues() {
        assertEquals('a', converters.convert("a", Character.class, "letter"));

        assertThrows(ConfigValueConvertException.class,
                () -> converters.convert("ab", Character.class, "letter"));
    }

    @Test
    void convertsEnumValues() {
        assertEquals(TestMode.ACTIVE, converters.convert("ACTIVE", TestMode.class, "mode"));
    }

    @Test
    void findsDefaultConverters() {
        assertTrue(converters.find(String.class).isPresent());
        assertTrue(converters.find(Integer.class).isPresent());
        assertTrue(converters.find(boolean.class).isPresent());
        assertTrue(converters.find(TestMode.class).isPresent());
        assertFalse(converters.find(UnsupportedType.class).isPresent());
    }

    @Test
    void canRegisterCustomConverter() {
        converters.register(new SimpleTypeConverter() {
            @Override
            public boolean supports(Class<?> targetType) {
                return targetType == CustomType.class;
            }

            @Override
            public Object convert(Object value, Class<?> targetType, SimpleTypeConvertContext context) {
                return new CustomType(String.valueOf(value));
            }
        });

        assertEquals("value", converters.convert("value", CustomType.class, "custom").value);
    }

    @Test
    void clearRestoresDefaultConvertersAndClearAllRemovesEverything() {
        converters.clearAll();
        assertFalse(converters.find(String.class).isPresent());

        converters.clear();
        assertTrue(converters.find(String.class).isPresent());
    }

    @Test
    void rejectsInvalidBooleanWithPath() {
        ConfigValueConvertException exception = assertThrows(ConfigValueConvertException.class,
                () -> converters.convert("maybe", Boolean.class, "enabled"));

        assertTrue(exception.getMessage().contains("enabled"));
    }

    @Test
    void rejectsUnsupportedTypeWithPath() {
        ConfigValueConvertException exception = assertThrows(ConfigValueConvertException.class,
                () -> converters.convert("value", UnsupportedType.class, "custom"));

        assertTrue(exception.getMessage().contains("custom"));
    }

    @Test
    void numberConverterRejectsUnsupportedNumberTypeWithConfigException() {
        NumberSimpleTypeConverter converter = new NumberSimpleTypeConverter();

        assertThrows(ConfigValueConvertException.class,
                () -> converter.convert(1, java.math.BigInteger.class, SimpleTypeConvertContext.of("amount")));
    }

    private enum TestMode {
        ACTIVE
    }

    private static class CustomType {
        private final String value;

        private CustomType(String value) {
            this.value = value;
        }
    }

    private static class UnsupportedType {
    }
}
