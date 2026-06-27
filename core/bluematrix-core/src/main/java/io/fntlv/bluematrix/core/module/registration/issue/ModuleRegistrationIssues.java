package io.fntlv.bluematrix.core.module.registration.issue;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class ModuleRegistrationIssues {
    private static final ModuleRegistrationIssues EMPTY = new ModuleRegistrationIssues(Collections.emptyList());

    private final List<ModuleRegistrationIssue> issues;

    public ModuleRegistrationIssues(List<ModuleRegistrationIssue> issues) {
        this.issues = immutableCopy(issues);
    }

    public static ModuleRegistrationIssues empty() {
        return EMPTY;
    }

    public static ModuleRegistrationIssues merge(ModuleRegistrationIssues... issues) {
        if (issues == null || issues.length == 0) {
            return empty();
        }
        List<ModuleRegistrationIssue> merged = new ArrayList<>();
        for (ModuleRegistrationIssues moduleIssues : issues) {
            if (moduleIssues != null) {
                merged.addAll(moduleIssues.all());
            }
        }
        return new ModuleRegistrationIssues(merged);
    }

    public List<ModuleRegistrationIssue> all() {
        return issues;
    }

    public boolean isEmpty() {
        return issues.isEmpty();
    }

    public int size() {
        return issues.size();
    }

    private static List<ModuleRegistrationIssue> immutableCopy(List<ModuleRegistrationIssue> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
