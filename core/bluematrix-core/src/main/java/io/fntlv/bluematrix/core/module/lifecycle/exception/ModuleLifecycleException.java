package io.fntlv.bluematrix.core.module.lifecycle.exception;

import io.fntlv.bluematrix.core.module.registration.exception.ModuleException;
import lombok.Getter;

public class ModuleLifecycleException extends ModuleException {
    @Getter
    private final String moduleId;

    public ModuleLifecycleException(String message, String moduleId, Throwable cause) {
        super(message, cause);
        this.moduleId = moduleId;
    }
}
