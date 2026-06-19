package io.fntlv.bluematrix.config.core.file.yaml;

import io.fntlv.bluematrix.config.core.file.exception.ConfigLoadException;
import io.fntlv.bluematrix.config.core.file.exception.ConfigSaveException;
import io.fntlv.bluematrix.config.core.section.ConfigSection;
import org.simpleyaml.configuration.comments.CommentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlConfigFileTest {

    @TempDir
    File tempDir;

    @Test
    void setDefaultCreatesFileWithComment() throws Exception {
        File file = new File(tempDir, "module/config.yml");
        YamlConfigFile document = new YamlConfigFile(file);

        document.setDefault("general.enable", true, "Whether to enable this module.");
        document.saveIfChanged();

        String content = new String(Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("general:"));
        assertTrue(content.contains("enable: true"));
        assertTrue(content.contains("Whether to enable this module."));
    }

    @Test
    void setDefaultDoesNotOverrideExistingValue() {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);

        document.set("join.enableWhiteList", true);
        document.save();

        YamlConfigFile reloaded = new YamlConfigFile(file);
        reloaded.setDefault("join.enableWhiteList", false);

        assertEquals(true, reloaded.get("join.enableWhiteList"));
        assertFalse(reloaded.hasChanged());
    }

    @Test
    void sectionUsesRelativePaths() {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);
        ConfigSection section = document.section("giftPacks.starter");

        section.setDefault("name", "Starter");

        assertEquals("Starter", document.get("giftPacks.starter.name"));
    }

    @Test
    void typedGettersReadValuesAndReturnDefaultsWithoutWriting() {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);
        document.set("values.name", "blue");
        document.set("values.enabled", "yes");
        document.set("values.amount", "3");
        document.set("values.longAmount", "4");
        document.set("values.ratio", "1.5");
        document.set("values.names", Arrays.asList("a", "b"));

        assertEquals("blue", document.getString("values.name"));
        assertTrue(document.getBoolean("values.enabled"));
        assertEquals(3, document.getInt("values.amount"));
        assertEquals(4L, document.getLong("values.longAmount"));
        assertEquals(1.5D, document.getDouble("values.ratio"));
        assertEquals(Arrays.asList("a", "b"), document.getStringList("values.names"));
        assertEquals("default", document.getString("missing", "default"));
        assertFalse(document.contains("missing"));
    }

    @Test
    void getOrSetDefaultWritesMissingValueAndKeepsExistingValue() {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);

        assertEquals("localhost", document.getOrSetDefault("database.host", "localhost"));
        assertTrue(document.hasChanged());
        document.saveIfChanged();

        assertEquals("localhost", document.getOrSetDefault("database.host", "remote"));
        assertFalse(document.hasChanged());
    }

    @Test
    void getOrSetDefaultWritesComment() throws Exception {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);

        document.getOrSetDefault("database.host", "localhost", "Database host");
        document.saveIfChanged();

        String content = new String(Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("Database host"));
    }

    @Test
    void lastModifiedTracksExternalFileChanges() throws Exception {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);

        document.set("general.enable", true);
        document.save();
        assertFalse(document.hasBeenModified());

        Thread.sleep(1100L);
        Files.write(file.toPath(), "general:\n  enable: false\n".getBytes());

        assertTrue(document.hasBeenModified());
        document.reload();
        assertFalse(document.hasBeenModified());
        assertFalse(document.getBoolean("general.enable"));
    }

    @Test
    void sectionTypedGettersAndDefaultsUseRelativePaths() {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);
        ConfigSection section = document.section("database");

        assertEquals("localhost", section.getOrSetDefault("host", "localhost"));
        section.set("port", "3306");

        assertEquals("localhost", document.getString("database.host"));
        assertEquals(3306, section.getInt("port"));
    }

    @Test
    void yamlMethodsReadKeysSectionsAndComments() {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);
        document.set("general.enable", true);
        document.set("general.database.host", "localhost");
        document.setComment("general.database", "Database settings", CommentType.BLOCK);

        Set<String> keys = document.getKeys("general");
        Set<String> deepKeys = document.getKeys("general", true);
        Set<YamlConfigSection> sections = document.getSections("general");

        assertTrue(keys.contains("enable"));
        assertTrue(keys.contains("database"));
        assertTrue(deepKeys.contains("database.host"));
        assertEquals("general.database", sections.iterator().next().getPath());
        assertTrue(document.isSection("general.database"));
        assertFalse(document.isSection("general.database.host"));
        assertEquals("Database settings", document.getComment("general.database", CommentType.BLOCK));
    }

    @Test
    void yamlSectionMethodsUseRelativePaths() {
        File file = new File(tempDir, "config.yml");
        YamlConfigFile document = new YamlConfigFile(file);
        YamlConfigSection section = document.section("general");
        section.set("database.host", "localhost");
        section.set("database.port", 3306);
        section.setComment("database", "Database settings", CommentType.BLOCK);

        assertTrue(section.getKeys().contains("database"));
        assertTrue(section.getKeys("database").contains("host"));
        assertEquals("general.database", section.getSection("database").getPath());
        assertEquals("general.database", section.getSections().iterator().next().getPath());
        assertTrue(section.isSection("database"));
        assertEquals("Database settings", section.getComment("database", CommentType.BLOCK));
    }

    @Test
    void directoryCreationFailureDuringLoadThrowsConfigLoadException() throws Exception {
        File parentFile = new File(tempDir, "not-a-directory");
        Files.write(parentFile.toPath(), "content".getBytes());

        assertThrows(ConfigLoadException.class, () -> new YamlConfigFile(new File(parentFile, "config.yml")));
    }

    @Test
    void directoryCreationFailureDuringSaveThrowsConfigSaveException() throws Exception {
        File parent = new File(tempDir, "parent");
        YamlConfigFile document = new YamlConfigFile(new File(parent, "config.yml"));
        assertTrue(parent.delete());
        Files.write(parent.toPath(), "content".getBytes());

        document.set("general.enable", true);

        assertThrows(ConfigSaveException.class, document::save);
    }
}
