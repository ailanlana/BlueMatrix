package io.fntlv.bluematrix.core.module.registration.issue.issues;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssueType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class CircularDependencyIssue extends ModuleRegistrationIssue {
    private final List<String> cycleModuleIds;

    public CircularDependencyIssue(ModuleCandidate module, String message, List<String> cycleModuleIds) {
        super(
                ModuleRegistrationIssueType.CIRCULAR_DEPENDENCY,
                module.id(),
                module.name(),
                message
        );
        this.cycleModuleIds = immutableCopy(cycleModuleIds);
    }

    public List<String> cycleModuleIds() {
        return cycleModuleIds;
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
