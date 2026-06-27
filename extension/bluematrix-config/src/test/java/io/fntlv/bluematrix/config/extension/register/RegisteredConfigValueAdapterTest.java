package io.fntlv.bluematrix.config.extension.register;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RegisteredConfigValueAdapterTest {

    @Test
    void storesEnumByName() {
        RegisteredConfigValueAdapter adapter = new RegisteredConfigValueAdapter();

        assertEquals("ACTIVE", adapter.toStoredValue(TestMode.ACTIVE));
    }

    @Test
    void storesEnumCollectionsByName() {
        RegisteredConfigValueAdapter adapter = new RegisteredConfigValueAdapter();

        assertEquals(Arrays.asList("ACTIVE", "PASSIVE"),
                adapter.toStoredValue(Arrays.asList(TestMode.ACTIVE, TestMode.PASSIVE)));
    }

    @Test
    void leavesNonEnumCollectionInstanceUnchanged() {
        RegisteredConfigValueAdapter adapter = new RegisteredConfigValueAdapter();
        java.util.List<Integer> values = Arrays.asList(1, 2);

        assertSame(values, adapter.toStoredValue(values));
    }

    private enum TestMode {
        ACTIVE,
        PASSIVE
    }
}
