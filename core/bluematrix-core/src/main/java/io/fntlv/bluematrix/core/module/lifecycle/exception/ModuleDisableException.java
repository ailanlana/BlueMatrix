package io.fntlv.bluematrix.core.module.lifecycle.exception;

public class ModuleDisableException extends ModuleLifecycleException {
    public ModuleDisableException(String moduleId, Throwable cause) {
        super("Failed to disable module: " + moduleId, moduleId, cause);
    }
}
