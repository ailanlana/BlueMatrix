package io.fntlv.bluematrix.core.module.registration.instance.parameter;

public class ModuleParameterResolutionException extends RuntimeException {

    public ModuleParameterResolutionException(String message) {
        super(message);
    }

    public ModuleParameterResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
