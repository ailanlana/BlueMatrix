package io.fntlv.bluematrix.lang.core.loader.pack;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.lang.core.loader.declaration.BlueLangDeclaredText;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BlueLangPack {
    public static final String LANG_FOLDER_NAME = "lang";

    private final File rootFolder;
    private final Map<String, ConfigFile> filesByLang = new LinkedHashMap<>();

    public BlueLangPack(File rootFolder) {
        this.rootFolder = rootFolder;
    }

    public BlueLangStoredText readOrSave(String path, BlueLangDeclaredText declaredText, boolean sectionStorage) {
        return readOrSave(path, declaredText.text(), declaredText.lang(), declaredText.extraDefaults(), sectionStorage);
    }

    public BlueLangStoredText readOrSave(String path,
                                         String defaultText,
                                         String lang,
                                         Map<String, Object> extraDefaults,
                                         boolean sectionStorage) {
        ConfigFile file = file(lang);
        String text = readOrSaveText(file, path, defaultText, sectionStorage);
        Map<String, Object> extras = sectionStorage
                ? readOrSaveExtras(file, path, extraDefaults)
                : new LinkedHashMap<>();
        return new BlueLangStoredText(text, lang, extras);
    }

    private String readOrSaveText(ConfigFile file, String path, String defaultText, boolean sectionStorage) {
        if (sectionStorage) {
            return readOrSaveSectionText(file, path, defaultText);
        }
        return readOrSaveScalarText(file, path, defaultText);
    }

    private String readOrSaveScalarText(ConfigFile file, String path, String defaultText) {
        if (file.contains(path) && !file.contains(path + ".text")) {
            return file.getString(path);
        }
        if (file.contains(path + ".text")) {
            return file.getString(path + ".text");
        }
        file.set(path, defaultText);
        return defaultText;
    }

    private Map<String, Object> readOrSaveExtras(ConfigFile file, String path, Map<String, Object> defaults) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String extraPath = path + "." + entry.getKey();
            if (!file.contains(extraPath)) {
                file.set(extraPath, entry.getValue());
            }
            values.put(entry.getKey(), file.get(extraPath));
        }
        return values;
    }

    public void saveIfChanged() {
        for (ConfigFile file : filesByLang.values()) {
            file.saveIfChanged();
        }
    }

    private ConfigFile file(String lang) {
        ConfigFile file = filesByLang.get(lang);
        if (file != null) {
            return file;
        }
        ConfigFile opened = Configs.yaml(new File(new File(rootFolder, LANG_FOLDER_NAME), lang + ".yml"));
        filesByLang.put(lang, opened);
        return opened;
    }

    private String readOrSaveSectionText(ConfigFile file, String path, String defaultText) {
        String textPath = path + ".text";
        if (file.contains(textPath)) {
            return file.getString(textPath);
        }
        if (file.contains(path) && isScalar(file.get(path))) {
            String value = file.getString(path);
            file.clear(path);
            file.set(textPath, value);
            return value;
        }
        file.set(textPath, defaultText);
        return defaultText;
    }

    private boolean isScalar(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character;
    }
}
