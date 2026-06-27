package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class ModuleRegistrationResult {
    private final List<ModuleContext> contexts;
    private final ModuleRegistrationIssues issues;

    public ModuleRegistrationResult(List<ModuleContext> contexts,
                                    ModuleRegistrationIssues issues) {
        this.contexts = immutableCopy(contexts);
        this.issues = issues == null ? ModuleRegistrationIssues.empty() : issues;
    }

    public static ModuleRegistrationResult success(List<ModuleContext> contexts) {
        return new ModuleRegistrationResult(contexts, ModuleRegistrationIssues.empty());
    }

    public List<ModuleContext> contexts() {
        return contexts;
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
