package io.fntlv.bluematrix.core.module.lifecycle.exception;

public class ModuleLoadException extends ModuleLifecycleException {
    public ModuleLoadException(String moduleId, Throwable cause) {
        super("Failed to load module: " + moduleId, moduleId, cause);
    }
}
