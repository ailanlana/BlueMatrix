package io.fntlv.bluematrix.lang.extension;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.lang.core.BlueLangText;
import io.fntlv.bluematrix.lang.core.LangType;
import io.fntlv.bluematrix.lang.core.annotation.BlueLang;
import io.fntlv.bluematrix.lang.core.annotation.BlueLangKey;
import io.fntlv.bluematrix.lang.core.loader.BlueLangLoader;
import io.fntlv.bluematrix.lang.extension.annotation.LangRegister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangModuleListenerTest {
    @TempDir
    File tempDir;

    @AfterEach
    void reset() {
        AutoLang.message = null;
        ExtendedLang.message = null;
    }

    @Test
    void loadsRegisteredLangClassIntoModuleFolder() {
        ModuleContext context = context(new LangModule());
        LangModuleListener listener = new LangModuleListener(tempDir, new BlueLangLoader());

        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        File zhFile = new File(tempDir, "lang-module/lang/" + LangType.ZH_CN + ".yml");
        File enFile = new File(tempDir, "lang-module/lang/" + LangType.EN_US + ".yml");
        assertEquals("你好", AutoLang.message.text());
        assertEquals("Hello", AutoLang.message.text(LangType.EN_US));
        assertEquals("你好", Configs.yaml(zhFile).getString("auto.message"));
        assertEquals("Hello", Configs.yaml(enFile).getString("auto.message"));
    }

    @Test
    void skipsModulesWithoutRegisteredLangClasses() {
        ModuleContext context = context(new PlainModule());
        LangModuleListener listener = new LangModuleListener(tempDir, new BlueLangLoader());

        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        assertFalse(new File(tempDir, "plain-module/lang").exists());
    }

    @Test
    void usesSharedLoaderRegisteredTextFactory() {
        ModuleContext context = context(new ExtendedLangModule());
        BlueLangLoader loader = new BlueLangLoader()
                .register(ExampleExtendedText.class, ExampleExtendedText::new);
        LangModuleListener listener = new LangModuleListener(tempDir, loader);

        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        File zhFile = new File(tempDir, "extended-lang-module/lang/" + LangType.ZH_CN + ".yml");
        assertEquals("扩展消息", ExtendedLang.message.text());
        assertEquals("打开菜单", ExtendedLang.message.hoverText());
        assertEquals("打开菜单", Configs.yaml(zhFile).getString("extended.message.hoverText"));
    }

    @Test
    void loadFailureReportsLoadPreError() {
        LangModuleListener listener = new LangModuleListener(tempDir, new BlueLangLoader());
        ModuleLoadEvent.Pre event = new ModuleLoadEvent.Pre(context(new BrokenLangModule()));

        listener.onLoadPre(event);

        assertTrue(event.hasError());
        assertEquals("lang", event.getErrorSource());
        assertEquals("Module language loading failed", event.getErrorMessage());
        assertTrue(event.getErrorCause() instanceof IllegalArgumentException);
        assertEquals("Duplicate lang: " + LangType.ZH_CN, event.getErrorCause().getMessage());
    }

    private ModuleContext context(Module module) {
        return new ModuleContext(module, module.getClass().getAnnotation(ModuleInfo.class));
    }

    @ModuleInfo(
            id = "lang-module",
            name = "Lang Module",
            scanPackages = "io.fntlv.bluematrix.lang.extension"
    )
    private static final class LangModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(
            id = "plain-module",
            name = "Plain Module",
            scanPackages = "io.fntlv.bluematrix.lang.extension.plain"
    )
    private static final class PlainModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(
            id = "extended-lang-module",
            name = "Extended Lang Module",
            scanPackages = "io.fntlv.bluematrix.lang.extension"
    )
    private static final class ExtendedLangModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(
            id = "broken-lang-module",
            name = "Broken Lang Module",
            scanPackages = "io.fntlv.bluematrix.lang.failure"
    )
    private static final class BrokenLangModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @LangRegister(defaultLang = LangType.ZH_CN)
    @BlueLangKey("auto")
    @SuppressWarnings("unused")
    public static final class AutoLang {
        @BlueLangKey("message")
        @BlueLang(text = "你好", lang = LangType.ZH_CN)
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private static BlueLangText message;
    }

    @LangRegister(defaultLang = LangType.ZH_CN)
    @BlueLangKey("extended")
    @SuppressWarnings("unused")
    public static final class ExtendedLang {
        @BlueLangKey("message")
        @BlueLang(
                text = "扩展消息",
                lang = LangType.ZH_CN,
                extras = @BlueLang.Extra(key = "hoverText", value = "打开菜单")
        )
        private static ExampleExtendedText message;
    }

    private static final class ExampleExtendedText {
        private final BlueLangText text;
        private final String hoverText;

        private ExampleExtendedText(BlueLangText text) {
            this.text = text;
            this.hoverText = text.data().extras().getString("hoverText", "");
        }

        private String text() {
            return text.text();
        }

        private String hoverText() {
            return hoverText;
        }
    }

}
