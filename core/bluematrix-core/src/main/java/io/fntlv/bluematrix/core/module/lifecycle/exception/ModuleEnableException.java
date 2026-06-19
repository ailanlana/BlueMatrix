package io.fntlv.bluematrix.core.module.lifecycle.exception;

public class ModuleEnableException extends ModuleLifecycleException {
    public ModuleEnableException(String moduleId, Throwable cause) {
        super("Failed to enable module: " + moduleId, moduleId, cause);
    }
}
