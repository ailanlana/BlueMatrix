package io.fntlv.bluematrix.config.core.file;

import io.fntlv.bluematrix.config.core.ConfigNode;

import java.io.File;
public interface ConfigFile extends ConfigNode {

    File getFile();

    void reload();

    void save();

    default void saveIfChanged() {
        if (hasChanged()) {
            save();
        }
    }

    boolean hasChanged();

    long lastModified();

    default boolean hasBeenModified() {
        return currentLastModified() != lastModified();
    }

    default long currentLastModified() {
        File file = getFile();
        return file.exists() ? file.lastModified() : 0L;
    }
}
