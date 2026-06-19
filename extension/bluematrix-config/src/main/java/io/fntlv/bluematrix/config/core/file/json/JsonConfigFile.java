package io.fntlv.bluematrix.config.core.file.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.exception.ConfigLoadException;
import io.fntlv.bluematrix.config.core.file.exception.ConfigSaveException;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.section.ConfigSection;
import io.fntlv.bluematrix.config.core.section.DefaultConfigSection;
import io.fntlv.bluematrix.config.core.type.complex.ComplexTypeHandler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class JsonConfigFile implements ConfigFile {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ReentrantLock lock = new ReentrantLock(true);
    private Map<String, Object> root = new LinkedHashMap<>();
    private boolean changed;
    private long lastModified;

    public JsonConfigFile(File file) {
        this.file = Objects.requireNonNull(file, "file");
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
            if (!file.exists() || file.length() == 0) {
                root = new LinkedHashMap<>();
                changed = false;
                lastModified = currentLastModified();
                return;
            }
            try (FileReader reader = new FileReader(file)) {
                Map<String, Object> loaded = gson.fromJson(reader, MAP_TYPE);
                root = loaded == null ? new LinkedHashMap<>() : loaded;
                changed = false;
                lastModified = currentLastModified();
            }
        } catch (IOException | JsonParseException e) {
            throw new ConfigLoadException("Failed to load JSON config: " + file.getAbsolutePath(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void save() {
        lock.lock();
        try {
            ensureParentDirectory();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(root, writer);
            }
            changed = false;
            lastModified = currentLastModified();
        } catch (IOException e) {
            throw new ConfigSaveException("Failed to save JSON config: " + file.getAbsolutePath(), e);
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
        return get(path) != null;
    }

    @Override
    public Object get(String path) {
        PathTarget target = resolve(path, false);
        if (target == null) {
            return null;
        }
        return target.parent.get(target.key);
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
        setRaw(path, value);
    }

    private void setRaw(String path, Object value) {
        PathTarget target = resolve(path, true);
        Object oldValue = target.parent.get(target.key);
        if (!Objects.equals(oldValue, value)) {
            target.parent.put(target.key, value);
            changed = true;
        }
    }

    @Override
    public void clear(String path) {
        PathTarget target = resolve(path, false);
        if (target != null && target.parent.containsKey(target.key)) {
            target.parent.remove(target.key);
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
        return getOrSetDefault(path, defaultValue);
    }

    @Override
    public void setComment(String path, String comment) {
    }

    @Override
    public ConfigSection section(String path) {
        return new DefaultConfigSection(this, path);
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

    private PathTarget resolve(String path, boolean create) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Config path cannot be empty");
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next == null) {
                if (!create) {
                    return null;
                }
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(parts[i], created);
                current = created;
                continue;
            }
            if (!(next instanceof Map)) {
                if (!create) {
                    return null;
                }
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(parts[i], created);
                current = created;
                continue;
            }
            current = (Map<String, Object>) next;
        }
        return new PathTarget(current, parts[parts.length - 1]);
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

    private static class PathTarget {
        private final Map<String, Object> parent;
        private final String key;

        private PathTarget(Map<String, Object> parent, String key) {
            this.parent = parent;
            this.key = key;
        }
    }
}
