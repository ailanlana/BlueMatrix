package io.fntlv.bluematrix.config.core.file.yaml;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.format.ConfigFileFormat;
import io.fntlv.bluematrix.config.core.format.ConfigFileFormats;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class YamlConfigFileFormat implements ConfigFileFormat {

    @Override
    public String name() {
        return ConfigFileFormats.YAML;
    }

    @Override
    public List<String> extensions() {
        return Collections.singletonList("yml");
    }

    @Override
    public boolean supportsComments() {
        return true;
    }

    @Override
    public ConfigFile open(File file) {
        return new YamlConfigFile(file);
    }
}
