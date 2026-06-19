package io.fntlv.bluematrix.config.extension.context;

import io.fntlv.bluematrix.core.module.Module;

public interface ModuleConfigContext {
    <T> T get(Class<T> type);

    String moduleId();

    Module module();
}
