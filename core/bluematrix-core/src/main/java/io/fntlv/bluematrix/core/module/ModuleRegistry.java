package io.fntlv.bluematrix.core.module;

import java.io.File;
import java.util.Optional;

public interface ModuleRegistry {

    <T extends Module> Optional<T> getModule(Class<T> clazz);

    boolean isEnabled(String moduleID);

    File getPath(Module module);
}
