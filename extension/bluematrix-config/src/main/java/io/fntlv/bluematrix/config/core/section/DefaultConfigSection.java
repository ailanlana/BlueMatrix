package io.fntlv.bluematrix.config.core.section;

import io.fntlv.bluematrix.config.core.file.ConfigFile;

import java.util.List;

public class DefaultConfigSection implements ConfigSection {

    protected final ConfigFile document;
    protected final String path;

    public DefaultConfigSection(ConfigFile document, String path) {
        this.document = document;
        this.path = path == null ? "" : path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean contains(String path) {
        return document.contains(resolve(path));
    }

    @Override
    public Object get(String path) {
        return document.get(resolve(path));
    }

    @Override
    public <T> T get(String path, Class<T> type) {
        return document.get(resolve(path), type);
    }

    @Override
    public <T> List<T> getList(String path, Class<T> elementType) {
        return document.getList(resolve(path), elementType);
    }

    @Override
    public String getString(String path) {
        return document.getString(resolve(path));
    }

    @Override
    public String getString(String path, String defaultValue) {
        return document.getString(resolve(path), defaultValue);
    }

    @Override
    public boolean getBoolean(String path) {
        return document.getBoolean(resolve(path));
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return document.getBoolean(resolve(path), defaultValue);
    }

    @Override
    public int getInt(String path) {
        return document.getInt(resolve(path));
    }

    @Override
    public int getInt(String path, int defaultValue) {
        return document.getInt(resolve(path), defaultValue);
    }

    @Override
    public long getLong(String path) {
        return document.getLong(resolve(path));
    }

    @Override
    public long getLong(String path, long defaultValue) {
        return document.getLong(resolve(path), defaultValue);
    }

    @Override
    public double getDouble(String path) {
        return document.getDouble(resolve(path));
    }

    @Override
    public double getDouble(String path, double defaultValue) {
        return document.getDouble(resolve(path), defaultValue);
    }

    @Override
    public List<String> getStringList(String path) {
        return document.getStringList(resolve(path));
    }

    @Override
    public List<String> getStringList(String path, List<String> defaultValue) {
        return document.getStringList(resolve(path), defaultValue);
    }

    @Override
    public void set(String path, Object value) {
        document.set(resolve(path), value);
    }

    @Override
    public void clear(String path) {
        document.clear(resolve(path));
    }

    @Override
    public void setDefault(String path, Object value) {
        document.setDefault(resolve(path), value);
    }

    @Override
    public void setDefault(String path, Object value, String comment) {
        document.setDefault(resolve(path), value, comment);
    }

    @Override
    public <T> T getOrSetDefault(String path, T defaultValue) {
        return document.getOrSetDefault(resolve(path), defaultValue);
    }

    @Override
    public <T> T getOrSetDefault(String path, T defaultValue, String comment) {
        return document.getOrSetDefault(resolve(path), defaultValue, comment);
    }

    @Override
    public void setComment(String path, String comment) {
        document.setComment(resolve(path), comment);
    }

    @Override
    public ConfigSection section(String path) {
        return document.section(resolve(path));
    }

    protected String resolve(String subPath) {
        if (subPath == null || subPath.isEmpty()) {
            return path;
        }
        if (path.isEmpty()) {
            return subPath;
        }
        return path + "." + subPath;
    }
}
