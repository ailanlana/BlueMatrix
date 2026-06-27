package io.fntlv.bluematrix.core.module.registration.issue.issues;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssueType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class MissingRequiredDependencyIssue extends ModuleRegistrationIssue {
    private final List<String> missingDependencyIds;

    public MissingRequiredDependencyIssue(ModuleCandidate module,
                                          String message,
                                          List<String> missingDependencyIds) {
        super(
                ModuleRegistrationIssueType.MISSING_REQUIRED_DEPENDENCY,
                module.getModuleInfo().id(),
                module.getModuleInfo().name(),
                message
        );
        this.missingDependencyIds = immutableCopy(missingDependencyIds);
    }

    public List<String> missingDependencyIds() {
        return missingDependencyIds;
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
