package io.fntlv.bluematrix.lang.core;

import io.fntlv.bluematrix.lang.core.BlueLangExtraValues;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlueLangExtraValuesTest {

    @Test
    void readsEnumValue() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("clickAction", "SUGGEST_COMMAND");

        BlueLangExtraValues extras = new BlueLangExtraValues(values);

        assertEquals(TestClickAction.SUGGEST_COMMAND,
                extras.getEnum("clickAction", TestClickAction.class, TestClickAction.RUN_COMMAND));
    }

    @Test
    void returnsDefaultEnumWhenValueIsMissing() {
        BlueLangExtraValues extras = new BlueLangExtraValues(new LinkedHashMap<>());

        assertEquals(TestClickAction.RUN_COMMAND,
                extras.getEnum("clickAction", TestClickAction.class, TestClickAction.RUN_COMMAND));
    }

    private enum TestClickAction {
        RUN_COMMAND,
        SUGGEST_COMMAND
    }
}
