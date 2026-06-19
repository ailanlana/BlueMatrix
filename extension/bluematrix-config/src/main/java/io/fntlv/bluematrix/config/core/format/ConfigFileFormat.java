package io.fntlv.bluematrix.config.core.format;

import io.fntlv.bluematrix.config.core.file.ConfigFile;

import java.io.File;
import java.util.List;

public interface ConfigFileFormat {

    String name();

    List<String> extensions();

    boolean supportsComments();

    ConfigFile open(File file);
}
