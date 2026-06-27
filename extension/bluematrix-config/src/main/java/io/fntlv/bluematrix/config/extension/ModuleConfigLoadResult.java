package io.fntlv.bluematrix.config.extension;

enum ModuleConfigLoadResult {
    ENABLED,
    DISABLED;

    boolean moduleEnabled() {
        return this == ENABLED;
    }
}
