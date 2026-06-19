package io.fntlv.bluematrix.config.core;

import io.fntlv.bluematrix.config.core.section.ConfigSection;

import java.util.List;

public interface ConfigNode {

    boolean contains(String path);

    Object get(String path);

    <T> T get(String path, Class<T> type);

    <T> List<T> getList(String path, Class<T> elementType);

    String getString(String path);

    String getString(String path, String defaultValue);

    boolean getBoolean(String path);

    boolean getBoolean(String path, boolean defaultValue);

    int getInt(String path);

    int getInt(String path, int defaultValue);

    long getLong(String path);

    long getLong(String path, long defaultValue);

    double getDouble(String path);

    double getDouble(String path, double defaultValue);

    List<String> getStringList(String path);

    List<String> getStringList(String path, List<String> defaultValue);

    void set(String path, Object value);

    void clear(String path);

    void setDefault(String path, Object value);

    void setDefault(String path, Object value, String comment);

    <T> T getOrSetDefault(String path, T defaultValue);

    <T> T getOrSetDefault(String path, T defaultValue, String comment);

    void setComment(String path, String comment);

    ConfigSection section(String path);
}
