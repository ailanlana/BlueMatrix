package io.fntlv.bluematrix.core.module.registration.outcome;

import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationStageResult;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues;

import java.util.ArrayList;
import java.util.List;

public final class RegistrationOutcomeCollector<T> {
    private final List<T> passed = new ArrayList<>();
    private final List<ModuleRegistrationIssue> issues = new ArrayList<>();

    public void pass(T value) {
        passed.add(value);
    }

    public void passAll(List<T> values) {
        passed.addAll(values);
    }

    public void issue(ModuleRegistrationIssue issue) {
        issues.add(issue);
    }

    public void issues(ModuleRegistrationIssues moduleIssues) {
        issues.addAll(moduleIssues.all());
    }

    public ModuleRegistrationStageResult<T> toStageResult() {
        return ModuleRegistrationStageResult.of(passed, new ModuleRegistrationIssues(issues));
    }
}
