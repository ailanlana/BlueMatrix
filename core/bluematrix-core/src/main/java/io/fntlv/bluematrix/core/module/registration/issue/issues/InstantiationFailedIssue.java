package io.fntlv.bluematrix.core.module.registration.issue.issues;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssueType;
import lombok.Getter;

@Getter
public final class InstantiationFailedIssue extends ModuleRegistrationIssue {
    private final Throwable cause;

    public InstantiationFailedIssue(ModuleCandidate module, String message, Throwable cause) {
        super(
                ModuleRegistrationIssueType.INSTANTIATION_FAILED,
                module.id(),
                module.name(),
                message
        );
        this.cause = cause;
    }

    public Throwable cause() {
        return cause;
    }
}
