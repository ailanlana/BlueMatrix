package io.fntlv.bluematrix.config.extension.context;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.ModuleConfigRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModuleConfigFilesTest {

    @TempDir
    File tempDir;

    @Test
    void returnsCachedConfigFileForEquivalentNames() {
        ModuleConfigFiles files = files();

        ConfigFile database = files.file("database");

        assertSame(database, files.file("database"));
        assertSame(database, files.file("database.yml"));
    }

    @Test
    void savesAllOpenedChangedFiles() {
        ModuleConfigFiles files = files();
        ConfigFile config = files.file();
        ConfigFile database = files.file("database");

        config.set("general.enable", true);
        database.set("database.port", 3306);

        files.saveIfChanged();

        assertEquals(true, Configs.yaml(new File(tempDir, "config.yml")).getBoolean("general.enable"));
        assertEquals(3306, Configs.yaml(new File(tempDir, "database.yml")).getInt("database.port"));
    }

    private ModuleConfigFiles files() {
        YamlConfigFileFormat format = new YamlConfigFileFormat();
        return new ModuleConfigFiles(fileName -> format.open(new File(
                tempDir,
                ModuleConfigRegistry.normalizeFileName(fileName)
        )));
    }
}
