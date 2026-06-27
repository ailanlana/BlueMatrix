package io.fntlv.bluematrix.core.module.registration.issue;

import lombok.Getter;

@Getter
public abstract class ModuleRegistrationIssue {
    private final ModuleRegistrationIssueType type;
    private final String moduleId;
    private final String moduleName;
    private final String message;

    protected ModuleRegistrationIssue(ModuleRegistrationIssueType type,
                                      String moduleId,
                                      String moduleName,
                                      String message) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message cannot be blank");
        }
        this.type = type;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.message = message;
    }

    public ModuleRegistrationIssueType type() {
        return type;
    }

    public String moduleId() {
        return moduleId;
    }

    public String moduleName() {
        return moduleName;
    }

    public String message() {
        return message;
    }
}
