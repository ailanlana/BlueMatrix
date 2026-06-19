package io.fntlv.bluematrix.core.module.instance.inject;

public class ModuleFieldInjectionException extends RuntimeException {

    public ModuleFieldInjectionException(String message) {
        super(message);
    }

    public ModuleFieldInjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
