package io.fntlv.bluematrix.core.module.registration.issue.issues;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssueType;
import lombok.Getter;

@Getter
public final class DuplicateModuleIdIssue extends ModuleRegistrationIssue {
    private final String duplicatedModuleId;
    private final String moduleClassName;

    public DuplicateModuleIdIssue(ModuleCandidate module, String message) {
        super(
                ModuleRegistrationIssueType.DUPLICATE_MODULE_ID,
                module.getModuleInfo().id(),
                module.getModuleInfo().name(),
                message
        );
        this.duplicatedModuleId = module.getModuleInfo().id();
        this.moduleClassName = module.getModuleClass().getName();
    }

    public String duplicatedModuleId() {
        return duplicatedModuleId;
    }

    public String moduleClassName() {
        return moduleClassName;
    }
}
