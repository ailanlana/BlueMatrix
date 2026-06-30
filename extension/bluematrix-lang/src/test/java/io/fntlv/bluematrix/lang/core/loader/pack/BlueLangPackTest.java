package io.fntlv.bluematrix.lang.core.loader.pack;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.lang.core.LangType;
import io.fntlv.bluematrix.lang.core.loader.declaration.BlueLangDeclaredText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueLangPackTest {
    @TempDir
    Path tempDir;

    @Test
    void readsOrSavesScalarText() {
        BlueLangPack pack = new BlueLangPack(tempDir.toFile());

        BlueLangStoredText storedText = pack.readOrSave(
                "command.help",
                declaredText("Hello", LangType.EN_US),
                false);
        pack.saveIfChanged();

        assertEquals("Hello", storedText.text());
        assertEquals("Hello", file(LangType.EN_US).getString("command.help"));
    }

    @Test
    void readsOrSavesSectionTextAndExtras() {
        BlueLangPack pack = new BlueLangPack(tempDir.toFile());
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("hoverText", Arrays.asList("Click here"));
        extras.put("runCommand", "/update");

        BlueLangStoredText storedText = pack.readOrSave(
                "update.available",
                declaredText("Update Available!", LangType.EN_US, extras),
                true);
        pack.saveIfChanged();

        ConfigFile file = file(LangType.EN_US);
        assertEquals("Update Available!", storedText.text());
        assertEquals("Click here", ((java.util.List<?>) storedText.extras().get("hoverText")).get(0));
        assertEquals("Update Available!", file.getString("update.available.text"));
        assertEquals("Click here", file.getStringList("update.available.hoverText").get(0));
        assertEquals("/update", file.getString("update.available.runCommand"));
    }

    @Test
    void migratesScalarTextToSectionText() {
        ConfigFile file = file(LangType.EN_US);
        file.set("update.available", "Existing scalar");
        file.save();
        BlueLangPack pack = new BlueLangPack(tempDir.toFile());

        BlueLangStoredText storedText = pack.readOrSave(
                "update.available",
                declaredText("Annotation text", LangType.EN_US),
                true);
        pack.saveIfChanged();

        ConfigFile saved = file(LangType.EN_US);
        assertEquals("Existing scalar", storedText.text());
        assertEquals("Existing scalar", saved.getString("update.available.text"));
        assertTrue(saved.contains("update.available.text"));
    }

    @Test
    void usesSeedExtrasWhenCompletingDefaultLang() {
        BlueLangPack pack = new BlueLangPack(tempDir.toFile());
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("hoverText", Arrays.asList("点击这里"));
        BlueLangStoredText seed = pack.readOrSave(
                "update.available",
                declaredText("发现新版本", LangType.ZH_CN, extras),
                true);

        BlueLangStoredText defaultText = pack.readOrSave(
                "update.available",
                seed.text(),
                LangType.EN_US,
                seed.extras(),
                true);
        pack.saveIfChanged();

        ConfigFile saved = file(LangType.EN_US);
        assertEquals("发现新版本", defaultText.text());
        assertEquals("点击这里", saved.getStringList("update.available.hoverText").get(0));
    }

    private BlueLangDeclaredText declaredText(String text, String lang) {
        return declaredText(text, lang, new LinkedHashMap<>());
    }

    private BlueLangDeclaredText declaredText(String text, String lang, Map<String, Object> extras) {
        return new BlueLangDeclaredText(text, lang, extras);
    }

    private ConfigFile file(String lang) {
        return Configs.yaml(new File(new File(tempDir.toFile(), BlueLangPack.LANG_FOLDER_NAME), lang + ".yml"));
    }
}
