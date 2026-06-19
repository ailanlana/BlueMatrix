package io.fntlv.bluematrix.config.extension.context;

import io.fntlv.bluematrix.config.core.file.ConfigFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class ModuleConfigFiles {
    private final Function<String, ConfigFile> fileResolver;
    private final Map<String, ConfigFile> files = new ConcurrentHashMap<>();

    ModuleConfigFiles(Function<String, ConfigFile> fileResolver) {
        if (fileResolver == null) {
            throw new IllegalArgumentException("fileResolver cannot be null");
        }
        this.fileResolver = fileResolver;
        file();
    }

    ConfigFile file() {
        return file("");
    }

    ConfigFile file(String fileName) {
        ConfigFile resolved = fileResolver.apply(fileName);
        String key = resolved.getFile().getAbsolutePath();
        ConfigFile existing = files.putIfAbsent(key, resolved);
        return existing == null ? resolved : existing;
    }

    void saveIfChanged() {
        for (ConfigFile file : files.values()) {
            file.saveIfChanged();
        }
    }
}
