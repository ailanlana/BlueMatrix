package io.fntlv.bluematrix.config.extension;

public final class ModuleConfigFileNames {
    public static final String DEFAULT_FILE_NAME = "config.yml";

    private ModuleConfigFileNames() {
    }

    public static String normalize(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return DEFAULT_FILE_NAME;
        }

        String normalized = fileName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Config file name cannot be blank");
        }
        if (normalized.contains("/") || normalized.contains("\\") || normalized.contains("..")) {
            throw new IllegalArgumentException("Config file name must be a simple file name: " + fileName);
        }
        if (normalized.endsWith(".yml")) {
            String baseName = normalized.substring(0, normalized.length() - ".yml".length());
            if (baseName.trim().isEmpty()) {
                throw new IllegalArgumentException("Config file name cannot be blank: " + fileName);
            }
            return normalized;
        }
        return normalized + ".yml";
    }
}
