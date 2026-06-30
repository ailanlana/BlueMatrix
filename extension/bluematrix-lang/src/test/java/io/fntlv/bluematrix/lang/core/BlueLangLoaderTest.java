package io.fntlv.bluematrix.lang.core;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.lang.core.annotation.BlueLang;
import io.fntlv.bluematrix.lang.core.annotation.BlueLangKey;
import io.fntlv.bluematrix.lang.core.loader.BlueLangLoader;
import io.fntlv.bluematrix.lang.core.BlueLangText;
import io.fntlv.bluematrix.lang.core.LangType;
import io.fntlv.bluematrix.logging.BlueLogLevel;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.logging.backend.BlueLogBackend;
import io.fntlv.bluematrix.logging.backend.BlueLogBackendProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueLangLoaderTest {
    private final BlueLogBackendProvider previousProvider = BlueLoggerFactory.getBackendProvider();

    @TempDir
    Path tempDir;

    @AfterEach
    void reset() {
        BlueLoggerFactory.setBackendProvider(previousProvider);
        GeneratedLang.commandHelp = null;
        ExistingLang.commandHelp = null;
        MissingKeyLang.commandHelp = null;
        DefaultKeyLang.HELP = null;
        FieldKeyLang.help = null;
        NoClassKeyLang.help = null;
        NoClassInferredKeyLang.COMMAND__HELP = null;
        GeneratedDefaultLang.commandHelp = null;
        ExistingDefaultLang.commandHelp = null;
        MultipleNonDefaultLang.commandHelp = null;
        ExtendedLang.update = null;
        MigratedExtendedLang.update = null;
        ExistingSectionLang.update = null;
        ExtendedDefaultLang.update = null;
        ExistingEnumExtraLang.update = null;
        InvalidFieldLang.commandHelp = null;
    }

    @Test
    void createsLangFilesAndAssignsStaticField() {
        File root = tempDir.toFile();

        List<BlueLangText> loaded = new BlueLangLoader().load(root, LangType.EN_US, GeneratedLang.class);

        assertEquals(1, loaded.size());
        assertEquals("command.help", GeneratedLang.commandHelp.key());
        assertEquals("使用 /demo help", GeneratedLang.commandHelp.text(LangType.ZH_CN));
        assertEquals("Use /demo help", GeneratedLang.commandHelp.text(LangType.EN_US));
        assertEquals("Use /demo help", GeneratedLang.commandHelp.text("ja_JP"));
        assertEquals("使用 /demo help", Configs.yaml(langFile(root, LangType.ZH_CN)).getString("command.help"));
        assertEquals("Use /demo help", Configs.yaml(langFile(root, LangType.EN_US)).getString("command.help"));
    }

    @Test
    void readsExistingFileValueInsteadOfAnnotationText() {
        File root = tempDir.toFile();
        ConfigFile file = Configs.yaml(langFile(root, LangType.ZH_CN));
        file.set("command.help", "文件里的中文");
        file.save();

        new BlueLangLoader().load(root, LangType.EN_US, ExistingLang.class);

        assertEquals("文件里的中文", ExistingLang.commandHelp.text(LangType.ZH_CN));
    }

    @Test
    void writesAnnotationTextWhenExistingFileMissesKey() {
        File root = tempDir.toFile();
        ConfigFile file = Configs.yaml(langFile(root, LangType.ZH_CN));
        file.set("other.key", "保留");
        file.save();

        new BlueLangLoader().load(root, LangType.EN_US, MissingKeyLang.class);

        assertEquals("使用 /demo help", MissingKeyLang.commandHelp.text(LangType.ZH_CN));
        assertEquals("使用 /demo help", Configs.yaml(langFile(root, LangType.ZH_CN)).getString("command.help"));
        assertEquals("保留", Configs.yaml(langFile(root, LangType.ZH_CN)).getString("other.key"));
    }

    @Test
    void usesClassKeyAndInferredFieldKey() {
        File root = tempDir.toFile();

        new BlueLangLoader().load(root, LangType.EN_US, DefaultKeyLang.class);

        assertEquals("command.help", DefaultKeyLang.HELP.key());
        assertEquals("Hello", Configs.yaml(langFile(root, LangType.EN_US)).getString("command.help"));
    }

    @Test
    void usesClassKeyAndFieldKeyAnnotations() {
        File root = tempDir.toFile();

        new BlueLangLoader().load(root, LangType.EN_US, FieldKeyLang.class);

        assertEquals("command.help", FieldKeyLang.help.key());
        assertEquals("Hello", Configs.yaml(langFile(root, LangType.EN_US)).getString("command.help"));
    }

    @Test
    void usesFieldKeyWhenClassKeyIsMissing() {
        File root = tempDir.toFile();

        new BlueLangLoader().load(root, LangType.EN_US, NoClassKeyLang.class);

        assertEquals("help", NoClassKeyLang.help.key());
        assertEquals("Hello", Configs.yaml(langFile(root, LangType.EN_US)).getString("help"));
    }

    @Test
    void infersNestedKeyFromFieldNameWhenClassKeyIsMissing() {
        File root = tempDir.toFile();

        new BlueLangLoader().load(root, LangType.EN_US, NoClassInferredKeyLang.class);

        assertEquals("command.help", NoClassInferredKeyLang.COMMAND__HELP.key());
        assertEquals("Hello", Configs.yaml(langFile(root, LangType.EN_US)).getString("command.help"));
    }

    @Test
    void createsDefaultLangFromFirstLoadedLangWhenMissing() {
        File root = tempDir.toFile();

        new BlueLangLoader().load(root, LangType.EN_US, GeneratedDefaultLang.class);

        assertEquals("使用 /demo help", GeneratedDefaultLang.commandHelp.text());
        assertEquals("使用 /demo help", GeneratedDefaultLang.commandHelp.text("ja_JP"));
        assertEquals("使用 /demo help", Configs.yaml(langFile(root, LangType.ZH_CN)).getString("command.help"));
        assertEquals("使用 /demo help", Configs.yaml(langFile(root, LangType.EN_US)).getString("command.help"));
    }

    @Test
    void readsExistingDefaultLangWhenCompletingMissingDefaultLang() {
        File root = tempDir.toFile();
        ConfigFile file = Configs.yaml(langFile(root, LangType.EN_US));
        file.set("command.help", "Existing default");
        file.save();

        new BlueLangLoader().load(root, LangType.EN_US, ExistingDefaultLang.class);

        assertEquals("Existing default", ExistingDefaultLang.commandHelp.text());
        assertEquals("Existing default", Configs.yaml(langFile(root, LangType.EN_US)).getString("command.help"));
    }

    @Test
    void usesFirstLoadedLangToCompleteDefaultLangWhenMultipleNonDefaultLangsExist() {
        File root = tempDir.toFile();
        ConfigFile file = Configs.yaml(langFile(root, LangType.ZH_CN));
        file.set("command.help", "文件里的中文");
        file.save();

        new BlueLangLoader().load(root, LangType.EN_US, MultipleNonDefaultLang.class);

        assertEquals("文件里的中文", MultipleNonDefaultLang.commandHelp.text());
        assertEquals("文件里的中文", Configs.yaml(langFile(root, LangType.EN_US)).getString("command.help"));
    }

    @Test
    void assignsRegisteredExtendedTextAndStoresSectionText() {
        File root = tempDir.toFile();

        new BlueLangLoader()
                .register(ExampleExtendedText.class, ExampleExtendedText::new)
                .load(root, LangType.EN_US, ExtendedLang.class);

        ConfigFile file = Configs.yaml(langFile(root, LangType.EN_US));
        assertEquals("Update Available!", ExtendedLang.update.text());
        assertEquals("Click here", ExtendedLang.update.hoverText().get(0));
        assertEquals("Update Available!", file.getString("update.available.text"));
        assertEquals("Click here", file.getStringList("update.available.hoverText").get(0));
        assertEquals("/update", file.getString("update.available.runCommand"));
        assertEquals(TestClickAction.RUN_COMMAND, ExtendedLang.update.clickAction());
        assertEquals(TestClickAction.RUN_COMMAND, file.get("update.available.clickAction", TestClickAction.class));
    }

    @Test
    void migratesScalarTextToSectionTextForRegisteredExtendedText() {
        File root = tempDir.toFile();
        ConfigFile file = Configs.yaml(langFile(root, LangType.EN_US));
        file.set("update.available", "Existing scalar");
        file.save();

        new BlueLangLoader()
                .register(ExampleExtendedText.class, ExampleExtendedText::new)
                .load(root, LangType.EN_US, MigratedExtendedLang.class);

        ConfigFile saved = Configs.yaml(langFile(root, LangType.EN_US));
        assertEquals("Existing scalar", MigratedExtendedLang.update.text());
        assertEquals("Existing scalar", saved.getString("update.available.text"));
        assertEquals("Click here", saved.getStringList("update.available.hoverText").get(0));
    }

    @Test
    void readsExistingSectionTextAndKeepsExtensionValues() {
        File root = tempDir.toFile();
        ConfigFile file = Configs.yaml(langFile(root, LangType.EN_US));
        file.set("update.available.text", "Existing section");
        file.set("update.available.hoverText", Arrays.asList("Existing hover"));
        file.save();

        new BlueLangLoader()
                .register(ExampleExtendedText.class, ExampleExtendedText::new)
                .load(root, LangType.EN_US, ExistingSectionLang.class);

        ConfigFile saved = Configs.yaml(langFile(root, LangType.EN_US));
        assertEquals("Existing section", ExistingSectionLang.update.text());
        assertEquals("Existing hover", ExistingSectionLang.update.hoverText().get(0));
        assertEquals("Existing hover", saved.getStringList("update.available.hoverText").get(0));
    }

    @Test
    void readsExistingEnumExtraValueInsteadOfAnnotationDefault() {
        File root = tempDir.toFile();
        ConfigFile file = Configs.yaml(langFile(root, LangType.EN_US));
        file.set("update.available.text", "Existing section");
        file.set("update.available.clickAction", "SUGGEST_COMMAND");
        file.save();

        new BlueLangLoader()
                .register(ExampleExtendedText.class, ExampleExtendedText::new)
                .load(root, LangType.EN_US, ExistingEnumExtraLang.class);

        assertEquals(TestClickAction.SUGGEST_COMMAND, ExistingEnumExtraLang.update.clickAction());
    }

    @Test
    void completesDefaultLangExtrasFromFirstLoadedLang() {
        File root = tempDir.toFile();

        new BlueLangLoader()
                .register(ExampleExtendedText.class, ExampleExtendedText::new)
                .load(root, LangType.EN_US, ExtendedDefaultLang.class);

        ConfigFile saved = Configs.yaml(langFile(root, LangType.EN_US));
        assertEquals("发现新版本", ExtendedDefaultLang.update.text());
        assertEquals("点击这里", ExtendedDefaultLang.update.hoverText().get(0));
        assertEquals("点击这里", saved.getStringList("update.available.hoverText").get(0));
    }

    @Test
    void warnsAndSkipsNonBlueLangTextField() {
        RecordingBackendProvider provider = new RecordingBackendProvider();
        BlueLoggerFactory.setBackendProvider(provider);

        List<BlueLangText> loaded = new BlueLangLoader().load(tempDir.toFile(), LangType.EN_US, InvalidFieldLang.class);

        assertEquals(0, loaded.size());
        assertTrue(provider.backend.messages.stream()
                .anyMatch(message -> message.contains("@BlueLang must be declared on BlueLangText field")));
    }

    @Test
    void warnsAndSkipsNonStaticBlueLangTextField() {
        RecordingBackendProvider provider = new RecordingBackendProvider();
        BlueLoggerFactory.setBackendProvider(provider);

        List<BlueLangText> loaded = new BlueLangLoader().load(tempDir.toFile(), LangType.EN_US, NonStaticLang.class);

        assertEquals(0, loaded.size());
        assertTrue(provider.backend.messages.stream()
                .anyMatch(message -> message.contains("@BlueLang BlueLangText field must be static")));
    }

    private File langFile(File root, String lang) {
        return new File(new File(root, BlueLangLoader.LANG_FOLDER_NAME), lang + ".yml");
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class GeneratedLang {
        @BlueLangKey("help")
        @BlueLang(text = "使用 /demo help", lang = LangType.ZH_CN)
        @BlueLang(text = "Use /demo help", lang = LangType.EN_US)
        private static BlueLangText commandHelp;
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class ExistingLang {
        @BlueLangKey("help")
        @BlueLang(text = "注解中文", lang = LangType.ZH_CN)
        private static BlueLangText commandHelp;
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class MissingKeyLang {
        @BlueLangKey("help")
        @BlueLang(text = "使用 /demo help", lang = LangType.ZH_CN)
        private static BlueLangText commandHelp;
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class DefaultKeyLang {
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private static BlueLangText HELP;
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class FieldKeyLang {
        @BlueLangKey("help")
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private static BlueLangText help;
    }

    @SuppressWarnings("unused")
    private static final class NoClassKeyLang {
        @BlueLangKey("help")
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private static BlueLangText help;
    }

    @SuppressWarnings("unused")
    private static final class NoClassInferredKeyLang {
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private static BlueLangText COMMAND__HELP;
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class GeneratedDefaultLang {
        @BlueLangKey("help")
        @BlueLang(text = "使用 /demo help", lang = LangType.ZH_CN)
        private static BlueLangText commandHelp;
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class ExistingDefaultLang {
        @BlueLangKey("help")
        @BlueLang(text = "使用 /demo help", lang = LangType.ZH_CN)
        private static BlueLangText commandHelp;
    }

    @BlueLangKey("command")
    @SuppressWarnings("unused")
    private static final class MultipleNonDefaultLang {
        @BlueLangKey("help")
        @BlueLang(text = "注解中文", lang = LangType.ZH_CN)
        @BlueLang(text = "日本語", lang = "ja_JP")
        private static BlueLangText commandHelp;
    }

    @BlueLangKey("update")
    @SuppressWarnings("unused")
    private static final class ExtendedLang {
        @BlueLangKey("available")
        @BlueLang(
                text = "Update Available!",
                lang = LangType.EN_US,
                extras = {
                        @BlueLang.Extra(key = "hoverText", value = {"Click here"}),
                        @BlueLang.Extra(key = "runCommand", value = "/update"),
                        @BlueLang.Extra(key = "clickAction", value = "RUN_COMMAND")
                }
        )
        private static ExampleExtendedText update;
    }

    @BlueLangKey("update")
    @SuppressWarnings("unused")
    private static final class MigratedExtendedLang {
        @BlueLangKey("available")
        @BlueLang(
                text = "Annotation text",
                lang = LangType.EN_US,
                extras = @BlueLang.Extra(key = "hoverText", value = {"Click here"})
        )
        private static ExampleExtendedText update;
    }

    @BlueLangKey("update")
    @SuppressWarnings("unused")
    private static final class ExistingSectionLang {
        @BlueLangKey("available")
        @BlueLang(
                text = "Annotation text",
                lang = LangType.EN_US,
                extras = @BlueLang.Extra(key = "hoverText", value = {"Click here"})
        )
        private static ExampleExtendedText update;
    }

    @BlueLangKey("update")
    @SuppressWarnings("unused")
    private static final class ExtendedDefaultLang {
        @BlueLangKey("available")
        @BlueLang(
                text = "发现新版本",
                lang = LangType.ZH_CN,
                extras = @BlueLang.Extra(key = "hoverText", value = {"点击这里"})
        )
        private static ExampleExtendedText update;
    }

    @BlueLangKey("update")
    @SuppressWarnings("unused")
    private static final class ExistingEnumExtraLang {
        @BlueLangKey("available")
        @BlueLang(
                text = "Annotation text",
                lang = LangType.EN_US,
                extras = @BlueLang.Extra(key = "clickAction", value = "RUN_COMMAND")
        )
        private static ExampleExtendedText update;
    }

    private static final class ExampleExtendedText {
        private final BlueLangText text;
        private final List<String> hoverText;
        private final TestClickAction clickAction;

        private ExampleExtendedText(BlueLangText text) {
            this.text = text;
            this.hoverText = text.data().extras().getStringList("hoverText");
            this.clickAction = text.data().extras()
                    .getEnum("clickAction", TestClickAction.class, TestClickAction.RUN_COMMAND);
        }

        private String text() {
            return text.text();
        }

        private List<String> hoverText() {
            return hoverText;
        }

        private TestClickAction clickAction() {
            return clickAction;
        }
    }

    private enum TestClickAction {
        RUN_COMMAND,
        SUGGEST_COMMAND
    }

    @SuppressWarnings("unused")
    private static final class InvalidFieldLang {
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private static String commandHelp;
    }

    @SuppressWarnings("unused")
    private static final class NonStaticLang {
        @BlueLang(text = "Hello", lang = LangType.EN_US)
        private BlueLangText commandHelp;
    }

    private static final class RecordingBackendProvider implements BlueLogBackendProvider {
        private final RecordingBackend backend = new RecordingBackend();

        @Override
        public BlueLogBackend getBackend(String name) {
            return backend;
        }
    }

    private static final class RecordingBackend implements BlueLogBackend {
        private final List<String> messages = new ArrayList<>();

        @Override
        public boolean isEnabled(BlueLogLevel level) {
            return true;
        }

        @Override
        public void log(BlueLogLevel level, String message) {
            messages.add(message);
        }

        @Override
        public void log(BlueLogLevel level, String message, Throwable throwable) {
            messages.add(message);
        }
    }
}
