package io.fntlv.bluematrix.config.core.format;

import io.fntlv.bluematrix.config.core.file.json.JsonConfigFileFormat;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import lombok.Getter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ConfigFileFormatRegistry {

    private final Map<String, ConfigFileFormat> formatsByName = new LinkedHashMap<>();
    private final Map<String, ConfigFileFormat> formatsByExtension = new LinkedHashMap<>();
    @Getter
    private ConfigFileFormat defaultFileFormat;

    public ConfigFileFormatRegistry() {
        registerDefaults();
        this.defaultFileFormat = getByName(ConfigFileFormats.YAML)
                .orElseThrow(() -> new IllegalStateException("Default YAML config format is not registered"));
    }

    private void registerDefaults() {
        register(new YamlConfigFileFormat());
        register(new JsonConfigFileFormat());
    }

    public void register(ConfigFileFormat format) {
        Objects.requireNonNull(format, "format");

        String name = normalize(format.name());
        if (formatsByName.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate config format name: " + name);
        }

        for (String extension : format.extensions()) {
            String normalizedExtension = normalize(extension);
            if (formatsByExtension.containsKey(normalizedExtension)) {
                throw new IllegalArgumentException("Duplicate config format extension: " + normalizedExtension);
            }
        }

        formatsByName.put(name, format);
        for (String extension : format.extensions()) {
            String normalizedExtension = normalize(extension);
            formatsByExtension.put(normalizedExtension, format);
        }
    }

    public Optional<ConfigFileFormat> getByName(String name) {
        return Optional.ofNullable(formatsByName.get(normalize(name)));
    }

    public Optional<ConfigFileFormat> getByExtension(String extension) {
        return Optional.ofNullable(formatsByExtension.get(normalize(extension)));
    }

    public Collection<ConfigFileFormat> getFileFormats() {
        return Collections.unmodifiableCollection(formatsByName.values());
    }

    public void setDefaultFileFormat(String name) {
        this.defaultFileFormat = getByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown config format: " + name));
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Config format value cannot be blank");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }
}
