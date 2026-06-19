package io.fntlv.bluematrix.config.core;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigsTest {

    @TempDir
    File tempDir;

    @Test
    void yamlLoadsAndSavesDocument() {
        File file = new File(tempDir, "reward.yml");
        ConfigFile document = Configs.yaml(file);

        document.set("reward.dayReward.daily.minutes", 30);
        document.save();

        ConfigFile reloaded = Configs.yaml(file);
        assertEquals(30, reloaded.get("reward.dayReward.daily.minutes"));
    }

    @Test
    void jsonLoadsAndSavesDocument() {
        File file = new File(tempDir, "reward.json");
        ConfigFile document = Configs.json(file);

        document.set("reward.dayReward.daily.minutes", 30);
        document.save();

        ConfigFile reloaded = Configs.json(file);
        assertEquals(30.0, reloaded.get("reward.dayReward.daily.minutes"));
    }

    @Test
    void autoDetectsYmlExtension() {
        ConfigFile yml = Configs.auto(new File(tempDir, "config.yml"));

        assertTrue(yml.getFile().getName().endsWith(".yml"));
    }

    @Test
    void autoRejectsYamlExtension() {
        assertThrows(IllegalArgumentException.class, () -> Configs.auto(new File(tempDir, "config.yaml")));
    }

    @Test
    void autoDetectsJsonExtension() {
        ConfigFile json = Configs.auto(new File(tempDir, "config.json"));

        assertTrue(json.getFile().getName().endsWith(".json"));
    }

    @Test
    void autoRejectsUnknownExtension() {
        assertThrows(IllegalArgumentException.class, () -> Configs.auto(new File(tempDir, "config.unknown")));
    }

    @Test
    void defaultFormatUsesYaml() {
        ConfigFile document = Configs.defaultFormat(new File(tempDir, "anything.conf"));

        document.set("general.enable", true);
        document.save();

        ConfigFile reloaded = Configs.yaml(new File(tempDir, "anything.conf"));
        assertEquals(true, reloaded.get("general.enable"));
    }
}
