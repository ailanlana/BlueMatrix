package io.fntlv.bluematrix.lang.core;

import io.fntlv.bluematrix.lang.core.LangType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangTypeTest {

    @Test
    void providesCommonLangNames() {
        assertEquals("en_US", LangType.EN_US);
        assertEquals("zh_CN", LangType.ZH_CN);
    }
}
