package io.fntlv.bluematrix.config.core;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.format.ConfigFileFormatRegistry;
import io.fntlv.bluematrix.config.core.format.ConfigFileFormats;
import io.fntlv.bluematrix.config.core.type.complex.ComplexTypeHandlerRegistry;
import io.fntlv.bluematrix.config.core.type.simple.SimpleTypeConverterRegistry;

import java.io.File;

public final class Configs {

    private static final ConfigFileFormatRegistry FILE_FORMAT_REGISTRY = new ConfigFileFormatRegistry();
    private static final SimpleTypeConverterRegistry SIMPLE_TYPE_CONVERTER_REGISTRY = new SimpleTypeConverterRegistry();
    private static final ComplexTypeHandlerRegistry TYPE_HANDLER_REGISTRY = new ComplexTypeHandlerRegistry();

    private Configs() {

    }

    public static ConfigFile yaml(File file) {
        return FILE_FORMAT_REGISTRY.getByName(ConfigFileFormats.YAML)
                .orElseThrow(() -> new IllegalStateException("YAML config format is not registered"))
                .open(file);
    }

    public static ConfigFile json(File file) {
        return FILE_FORMAT_REGISTRY.getByName(ConfigFileFormats.JSON)
                .orElseThrow(() -> new IllegalStateException("JSON config format is not registered"))
                .open(file);
    }

    public static ConfigFile auto(File file) {
        return open(file);
    }

    public static ConfigFile open(File file) {
        String extension = getExtension(file);
        return FILE_FORMAT_REGISTRY.getByExtension(extension)
                .orElseThrow(() -> new IllegalArgumentException("Unknown config file extension: " + extension))
                .open(file);
    }

    public static ConfigFile defaultFormat(File file) {
        return FILE_FORMAT_REGISTRY.getDefaultFileFormat().open(file);
    }

    public static ConfigFileFormatRegistry fileFormatRegistry() {
        return FILE_FORMAT_REGISTRY;
    }

    public static ComplexTypeHandlerRegistry typeHandlers() {
        return TYPE_HANDLER_REGISTRY;
    }

    public static SimpleTypeConverterRegistry simpleTypeConverters() {
        return SIMPLE_TYPE_CONVERTER_REGISTRY;
    }

    private static String getExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("Config file has no extension: " + file.getAbsolutePath());
        }
        return fileName.substring(dotIndex + 1);
    }
}
