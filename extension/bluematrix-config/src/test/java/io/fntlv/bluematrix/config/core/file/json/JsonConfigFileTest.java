package io.fntlv.bluematrix.config.core.file.json;

import io.fntlv.bluematrix.config.core.file.exception.ConfigLoadException;
import io.fntlv.bluematrix.config.core.file.exception.ConfigSaveException;
import io.fntlv.bluematrix.config.core.section.ConfigSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonConfigFileTest {

    @TempDir
    File tempDir;

    @Test
    void setDefaultCreatesNestedJsonFile() throws Exception {
        File file = new File(tempDir, "module/config.json");
        JsonConfigFile document = new JsonConfigFile(file);

        document.setDefault("general.enable", true, "Ignored JSON comment");
        document.saveIfChanged();

        String content = new String(Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("\"general\""));
        assertTrue(content.contains("\"enable\""));
        assertTrue(content.contains("true"));
    }

    @Test
    void setDefaultDoesNotOverrideExistingValue() {
        File file = new File(tempDir, "config.json");
        JsonConfigFile document = new JsonConfigFile(file);

        document.set("join.enableWhiteList", true);
        document.save();

        JsonConfigFile reloaded = new JsonConfigFile(file);
        reloaded.setDefault("join.enableWhiteList", false);

        assertEquals(true, reloaded.get("join.enableWhiteList"));
        assertFalse(reloaded.hasChanged());
    }

    @Test
    void sectionUsesRelativePaths() {
        File file = new File(tempDir, "config.json");
        JsonConfigFile document = new JsonConfigFile(file);
        ConfigSection section = document.section("giftPacks.starter");

        section.setDefault("name", "Starter");

        assertEquals("Starter", document.get("giftPacks.starter.name"));
    }

    @Test
    void saveReloadsValues() {
        File file = new File(tempDir, "config.json");
        JsonConfigFile document = new JsonConfigFile(file);

        document.set("reward.dayReward.daily.minutes", 30);
        document.save();

        JsonConfigFile reloaded = new JsonConfigFile(file);
        assertEquals(30.0, reloaded.get("reward.dayReward.daily.minutes"));
    }

    @Test
    void setCommentDoesNothing() {
        File file = new File(tempDir, "config.json");
        JsonConfigFile document = new JsonConfigFile(file);

        document.setComment("general.enable", "Ignored JSON comment");

        assertFalse(document.hasChanged());
    }

    @Test
    void typedGettersReadValuesAndReturnDefaultsWithoutWriting() {
        File file = new File(tempDir, "config.json");
        JsonConfigFile document = new JsonConfigFile(file);
        document.set("values.name", "blue");
        document.set("values.enabled", "yes");
        document.set("values.amount", 3.0D);
        document.set("values.longAmount", 4.0D);
        document.set("values.ratio", 1.5D);
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
        File file = new File(tempDir, "config.json");
        JsonConfigFile document = new JsonConfigFile(file);

        assertEquals("localhost", document.getOrSetDefault("database.host", "localhost"));
        assertTrue(document.hasChanged());
        document.saveIfChanged();

        assertEquals("localhost", document.getOrSetDefault("database.host", "remote"));
        assertFalse(document.hasChanged());
    }

    @Test
    void lastModifiedTracksExternalFileChanges() throws Exception {
        File file = new File(tempDir, "config.json");
        JsonConfigFile document = new JsonConfigFile(file);

        document.set("general.enable", true);
        document.save();
        assertFalse(document.hasBeenModified());

        Thread.sleep(1100L);
        Files.write(file.toPath(), "{\"general\":{\"enable\":false}}".getBytes());

        assertTrue(document.hasBeenModified());
        document.reload();
        assertFalse(document.hasBeenModified());
        assertFalse(document.getBoolean("general.enable"));
    }

    @Test
    void sectionTypedGettersAndDefaultsUseRelativePaths() {
        File file = new File(tempDir, "config.json");
        JsonConfigFile document = new JsonConfigFile(file);
        ConfigSection section = document.section("database");

        assertEquals("localhost", section.getOrSetDefault("host", "localhost"));
        section.set("port", "3306");

        assertEquals("localhost", document.getString("database.host"));
        assertEquals(3306, section.getInt("port"));
    }

    @Test
    void directoryCreationFailureDuringLoadThrowsConfigLoadException() throws Exception {
        File parentFile = new File(tempDir, "not-a-directory");
        Files.write(parentFile.toPath(), "content".getBytes());

        assertThrows(ConfigLoadException.class, () -> new JsonConfigFile(new File(parentFile, "config.json")));
    }

    @Test
    void directoryCreationFailureDuringSaveThrowsConfigSaveException() throws Exception {
        File parent = new File(tempDir, "parent");
        JsonConfigFile document = new JsonConfigFile(new File(parent, "config.json"));
        assertTrue(parent.delete());
        Files.write(parent.toPath(), "content".getBytes());

        document.set("general.enable", true);

        assertThrows(ConfigSaveException.class, document::save);
    }
}
