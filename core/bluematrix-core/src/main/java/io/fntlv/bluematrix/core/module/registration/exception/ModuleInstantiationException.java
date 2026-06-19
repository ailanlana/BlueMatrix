package io.fntlv.bluematrix.core.module.registration.exception;

public class ModuleInstantiationException extends ModuleException {
    public ModuleInstantiationException(String moduleId, Throwable cause) {
        super("Failed to instantiate module: " + moduleId, cause);
    }
}
