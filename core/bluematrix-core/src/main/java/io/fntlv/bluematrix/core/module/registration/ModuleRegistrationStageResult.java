package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class ModuleRegistrationStageResult<T> {
    private final List<T> passed;
    private final ModuleRegistrationIssues issues;

    private ModuleRegistrationStageResult(List<T> passed, ModuleRegistrationIssues issues) {
        this.passed = immutableCopy(passed);
        this.issues = issues == null ? ModuleRegistrationIssues.empty() : issues;
    }

    public static <T> ModuleRegistrationStageResult<T> of(List<T> passed, ModuleRegistrationIssues issue) {
        return new ModuleRegistrationStageResult<>(passed, issue);
    }

    public static <T> ModuleRegistrationStageResult<T> of(List<T> passed) {
        return new ModuleRegistrationStageResult<>(passed, ModuleRegistrationIssues.empty());
    }

    public static <T> ModuleRegistrationStageResult<T> empty() {
        return new ModuleRegistrationStageResult<>(Collections.emptyList(), ModuleRegistrationIssues.empty());
    }

    public List<T> passed() {
        return passed;
    }

    public ModuleRegistrationIssues issues() {
        return issues;
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
