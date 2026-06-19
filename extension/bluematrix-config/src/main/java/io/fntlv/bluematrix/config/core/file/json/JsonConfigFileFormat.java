package io.fntlv.bluematrix.config.core.file.json;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.format.ConfigFileFormat;
import io.fntlv.bluematrix.config.core.format.ConfigFileFormats;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class JsonConfigFileFormat implements ConfigFileFormat {

    @Override
    public String name() {
        return ConfigFileFormats.JSON;
    }

    @Override
    public List<String> extensions() {
        return Collections.singletonList("json");
    }

    @Override
    public boolean supportsComments() {
        return false;
    }

    @Override
    public ConfigFile open(File file) {
        return new JsonConfigFile(file);
    }
}
