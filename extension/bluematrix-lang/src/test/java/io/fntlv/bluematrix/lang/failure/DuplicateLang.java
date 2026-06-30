package io.fntlv.bluematrix.lang.failure;

import io.fntlv.bluematrix.lang.core.BlueLangText;
import io.fntlv.bluematrix.lang.core.LangType;
import io.fntlv.bluematrix.lang.core.annotation.BlueLang;
import io.fntlv.bluematrix.lang.core.annotation.BlueLangKey;
import io.fntlv.bluematrix.lang.extension.annotation.LangRegister;

@LangRegister(defaultLang = LangType.ZH_CN)
@BlueLangKey("duplicate")
@SuppressWarnings("unused")
public final class DuplicateLang {
    @BlueLangKey("message")
    @BlueLang(text = "第一条", lang = LangType.ZH_CN)
    @BlueLang(text = "第二条", lang = LangType.ZH_CN)
    private static BlueLangText message;

    private DuplicateLang() {
    }
}
