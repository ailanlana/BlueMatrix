package io.fntlv.bluematrix.config.core.file.yaml;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.exception.ConfigLoadException;
import io.fntlv.bluematrix.config.core.file.exception.ConfigSaveException;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.type.complex.ComplexTypeHandler;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.comments.CommentType;
import org.simpleyaml.configuration.comments.format.YamlCommentFormat;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.configuration.implementation.SimpleYamlImplementation;
import org.simpleyaml.configuration.implementation.api.QuoteStyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class YamlConfigFile implements ConfigFile {

    private final File file;
    private final YamlFile yamlFile;

    private final ReentrantLock lock = new ReentrantLock(true);
    private boolean changed;
    private long lastModified;


    public YamlConfigFile(File file) {
        this.file = Objects.requireNonNull(file, "file");
        this.yamlFile = createYamlFile(file);
        reload();
    }

    // ConfigFile

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void reload() {
        lock.lock();
        try {
            ensureParentDirectory();
            if (file.exists()) {
                yamlFile.loadWithComments();
            } else {
                yamlFile.loadFromString("");
            }
            changed = false;
            lastModified = currentLastModified();
        } catch (Exception e) {
            throw new ConfigLoadException("Failed to load YAML config: " + file.getAbsolutePath(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void save() {
        lock.lock();
        try {
            ensureParentDirectory();
            yamlFile.save(file);
            changed = false;
            lastModified = currentLastModified();
        } catch (Exception e) {
            throw new ConfigSaveException("Failed to save YAML config: " + file.getAbsolutePath(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    // ConfigNode

    @Override
    public boolean contains(String path) {
        return yamlFile.contains(path);
    }

    @Override
    public Object get(String path) {
        return yamlFile.get(path);
    }

    @Override
    public <T> T get(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (!contains(path)) {
            return null;
        }
        Object value = get(path);
        if (value != null && Configs.simpleTypeConverters().supports(type)) {
            return Configs.simpleTypeConverters().convert(value, type, path);
        }
        T handledValue = loadHandledValue(path, type);
        if (handledValue != null) {
            return handledValue;
        }
        return null;
    }

    @Override
    public <T> List<T> getList(String path, Class<T> elementType) {
        Objects.requireNonNull(elementType, "elementType");
        if (!contains(path)) {
            return new ArrayList<>();
        }
        Object value = get(path);
        if (Configs.simpleTypeConverters().supports(elementType)) {
            return convertSimpleList(path, elementType, value);
        }
        List<T> handledList = loadHandledList(path, elementType);
        if (handledList != null) {
            return handledList;
        }
        return new ArrayList<>();
    }

    private <T> List<T> convertSimpleList(String path, Class<T> elementType, Object value) {
        List<T> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                result.add(Configs.simpleTypeConverters().convert(item, elementType, path));
            }
            return result;
        }
        result.add(Configs.simpleTypeConverters().convert(value, elementType, path));
        return result;
    }

    @Override
    public String getString(String path) {
        return getString(path, null);
    }

    @Override
    public String getString(String path, String defaultValue) {
        Object value = get(path);
        return value == null ? defaultValue : Configs.simpleTypeConverters().convert(value, String.class, path);
    }

    @Override
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path);
        return value == null ? defaultValue : Configs.simpleTypeConverters().convert(value, Boolean.class, path);
    }

    @Override
    public int getInt(String path) {
        return getInt(path, 0);
    }

    @Override
    public int getInt(String path, int defaultValue) {
        Object value = get(path);
        return value == null ? defaultValue : Configs.simpleTypeConverters().convert(value, Integer.class, path);
    }

    @Override
    public long getLong(String path) {
        return getLong(path, 0L);
    }

    @Override
    public long getLong(String path, long defaultValue) {
        Object value = get(path);
        return value == null ? defaultValue : Configs.simpleTypeConverters().convert(value, Long.class, path);
    }

    @Override
    public double getDouble(String path) {
        return getDouble(path, 0D);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
        Object value = get(path);
        return value == null ? defaultValue : Configs.simpleTypeConverters().convert(value, Double.class, path);
    }

    @Override
    public List<String> getStringList(String path) {
        return getStringList(path, null);
    }

    @Override
    public List<String> getStringList(String path, List<String> defaultValue) {
        Object value = get(path);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof List)) {
            List<String> result = new ArrayList<>();
            result.add(Configs.simpleTypeConverters().convert(value, String.class, path));
            return result;
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            result.add(Configs.simpleTypeConverters().convert(item, String.class, path));
        }
        return result;
    }

    @Override
    public void set(String path, Object value) {
        if (value != null && saveHandledValue(path, value)) {
            return;
        }
        Object oldValue = yamlFile.get(path);
        if (!Objects.equals(oldValue, value)) {
            yamlFile.set(path, value);
            changed = true;
        }
    }

    @Override
    public void clear(String path) {
        if (contains(path)) {
            yamlFile.set(path, null);
            changed = true;
        }
    }

    @Override
    public void setDefault(String path, Object value) {
        if (!contains(path)) {
            set(path, value);
        }
    }

    @Override
    public void setDefault(String path, Object value, String comment) {
        setDefault(path, value);
        setComment(path, comment);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrSetDefault(String path, T defaultValue) {
        if (!contains(path)) {
            set(path, defaultValue);
            return defaultValue;
        }
        Object value = get(path);
        if (value == null || defaultValue == null || defaultValue instanceof List) {
            if (defaultValue instanceof List) {
                T handledList = loadHandledList(path, defaultValue);
                return handledList == null ? (T) value : handledList;
            }
            return (T) value;
        }
        T handledValue = loadHandledValue(path, defaultValue);
        if (handledValue != null) {
            return handledValue;
        }
        return (T) Configs.simpleTypeConverters().convert(value, defaultValue.getClass(), path);
    }

    @Override
    public <T> T getOrSetDefault(String path, T defaultValue, String comment) {
        T value = getOrSetDefault(path, defaultValue);
        setComment(path, comment);
        return value;
    }

    @Override
    public void setComment(String path, String comment) {
        if (comment == null || comment.isEmpty()) {
            return;
        }
        String oldComment = yamlFile.getComment(path);
        if (!comment.equals(oldComment)) {
            yamlFile.setComment(path, comment);
            changed = true;
        }
    }

    @Override
    public YamlConfigSection section(String path) {
        return getSection(path);
    }

    // Yaml

    public YamlConfigSection getSection(String path) {
        return new YamlConfigSection(this, path);
    }

    public Set<String> getKeys(String path) {
        return getKeys(path, false);
    }

    public Set<String> getKeys(String path, boolean deep) {
        if (isRoot(path)) {
            return new LinkedHashSet<>(yamlFile.getKeys(deep));
        }
        if (!isSection(path)) {
            return new LinkedHashSet<>();
        }
        ConfigurationSection section = yamlFile.getConfigurationSection(path);
        return section == null ? new LinkedHashSet<>() : new LinkedHashSet<>(section.getKeys(deep));
    }

    public Set<YamlConfigSection> getSections(String path) {
        return getSections(path, false);
    }

    public Set<YamlConfigSection> getSections(String path, boolean deep) {
        Set<YamlConfigSection> sections = new LinkedHashSet<>();
        for (String key : getKeys(path, deep)) {
            String sectionPath = appendPath(path, key);
            if (isSection(sectionPath)) {
                sections.add(getSection(sectionPath));
            }
        }
        return sections;
    }

    public String getComment(String path) {
        return yamlFile.getComment(path);
    }

    public String getComment(String path, CommentType type) {
        return yamlFile.getComment(path, type);
    }

    public void setComment(String path, String comment, CommentType type) {
        String oldComment = yamlFile.getComment(path, type);
        if (!Objects.equals(oldComment, comment)) {
            yamlFile.setComment(path, comment, type);
            changed = true;
        }
    }

    public boolean isSection(String path) {
        return isRoot(path) || yamlFile.isConfigurationSection(path);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean saveHandledValue(String path, Object value) {
        Optional<ComplexTypeHandler<?>> direct = Configs.typeHandlers().find(value.getClass());
        if (direct.isPresent() && direct.get().canSave()) {
            ComplexTypeHandler handler = direct.get();
            handler.save(section(path), value);
            return true;
        }

        if (value instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) value;
            List<Object> values = new ArrayList<>();
            for (Object item : iterable) {
                values.add(item);
            }
            if (values.isEmpty() || values.get(0) == null) {
                return false;
            }
            Optional<ComplexTypeHandler<?>> found = Configs.typeHandlers().find(values.get(0).getClass());
            if (!found.isPresent()) {
                return false;
            }
            ComplexTypeHandler handler = found.get();
            if (!handler.canSave()) {
                return false;
            }
            if (handler.canSerializeToStringList()) {
                List<String> serialized = new ArrayList<>();
                for (Object item : values) {
                    serialized.add(handler.serializeString(item));
                }
                setRaw(path, serialized);
                return true;
            }
            clear(path);
            for (int index = 0; index < values.size(); index++) {
                handler.save(section(path + "." + index), values.get(index));
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T loadHandledValue(String path, T defaultValue) {
        return loadHandledValue(path, (Class<T>) defaultValue.getClass());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T loadHandledValue(String path, Class<T> type) {
        Optional<ComplexTypeHandler<?>> found = Configs.typeHandlers().find(type);
        if (!found.isPresent() || !found.get().canLoad()) {
            return null;
        }
        ComplexTypeHandler handler = found.get();
        return (T) handler.load(section(path), type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T loadHandledList(String path, T defaultValue) {
        List<?> defaults = (List<?>) defaultValue;
        if (defaults.isEmpty() || defaults.get(0) == null) {
            return null;
        }
        Optional<ComplexTypeHandler<?>> found = Configs.typeHandlers().find(defaults.get(0).getClass());
        if (!found.isPresent() || !found.get().canLoad()) {
            return null;
        }
        ComplexTypeHandler handler = found.get();
        List<Object> loaded = new ArrayList<>();
        if (handler.canSerializeToStringList()) {
            List<String> stored = getStringList(path);
            if (stored == null) {
                return null;
            }
            for (String item : stored) {
                loaded.add(handler.deserializeString(item, defaults.get(0).getClass()));
            }
            return (T) loaded;
        }
        for (int index = 0; contains(path + "." + index); index++) {
            loaded.add(handler.load(section(path + "." + index), defaults.get(0).getClass()));
        }
        return (T) loaded;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<T> loadHandledList(String path, Class<T> elementType) {
        Optional<ComplexTypeHandler<?>> found = Configs.typeHandlers().find(elementType);
        if (!found.isPresent() || !found.get().canLoad()) {
            return null;
        }
        ComplexTypeHandler handler = found.get();
        List<T> loaded = new ArrayList<>();
        if (handler.canSerializeToStringList()) {
            List<String> stored = getStringList(path);
            if (stored == null) {
                return null;
            }
            for (String item : stored) {
                loaded.add((T) handler.deserializeString(item, elementType));
            }
            return loaded;
        }
        for (int index = 0; contains(path + "." + index); index++) {
            loaded.add((T) handler.load(section(path + "." + index), elementType));
        }
        return loaded;
    }

    private void setRaw(String path, Object value) {
        Object oldValue = yamlFile.get(path);
        if (!Objects.equals(oldValue, value)) {
            yamlFile.set(path, value);
            changed = true;
        }
    }

    private boolean isRoot(String path) {
        return path == null || path.isEmpty();
    }

    private String appendPath(String path, String key) {
        if (isRoot(path)) {
            return key;
        }
        return path + "." + key;
    }

    private void ensureParentDirectory() throws IOException {
        File parent = file.getParentFile();
        if (parent == null) {
            return;
        }
        if (parent.exists() && !parent.isDirectory()) {
            throw new IOException("Config directory path is not a directory: " + parent.getAbsolutePath());
        }
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create config directory: " + parent.getAbsolutePath());
        }
    }

    private YamlFile createYamlFile(File file) {
        SimpleYamlImplementation implementation = new SimpleYamlImplementation();
        YamlFile created = new YamlFile(implementation);
        created.setConfigurationFile(file);
        created.setCommentFormat(YamlCommentFormat.PRETTY);
        created.options().useComments(true);
        created.options().quoteStyleDefaults().setDefaultQuoteStyle(QuoteStyle.PLAIN);
        created.options().quoteStyleDefaults().setQuoteStyle(List.class, QuoteStyle.DOUBLE);
        created.options().quoteStyleDefaults().setQuoteStyle(String.class, QuoteStyle.DOUBLE);
        implementation.getDumperOptions().setSplitLines(false);
        implementation.getDumperOptions().setProcessComments(true);
        implementation.getLoaderOptions().setProcessComments(true);
        return created;
    }
}
