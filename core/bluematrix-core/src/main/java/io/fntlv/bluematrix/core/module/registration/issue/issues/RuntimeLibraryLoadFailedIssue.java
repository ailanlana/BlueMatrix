package io.fntlv.bluematrix.core.module.registration.issue.issues;

import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoadResult;
import io.fntlv.bluematrix.core.module.ModuleDescriptor;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssueType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class RuntimeLibraryLoadFailedIssue extends ModuleRegistrationIssue {
    private final String moduleClassName;
    private final List<String> failedLibraries;
    private final Throwable cause;

    public RuntimeLibraryLoadFailedIssue(ModuleDescriptor descriptor,
                                         Class<?> moduleClass,
                                         ModuleRuntimeLibraryLoadResult result,
                                         String message) {
        this(
                descriptor.id(),
                descriptor.name(),
                moduleClass.getName(),
                result,
                message
        );
    }

    public RuntimeLibraryLoadFailedIssue(String moduleId,
                                         String moduleName,
                                         String moduleClassName,
                                         ModuleRuntimeLibraryLoadResult result,
                                         String message) {
        super(
                ModuleRegistrationIssueType.RUNTIME_LIBRARY_LOAD_FAILED,
                moduleId,
                moduleName,
                message
        );
        this.moduleClassName = moduleClassName;
        this.failedLibraries = immutableCopy(result == null ? Collections.emptyList() : result.failedLibraries());
        this.cause = result == null ? null : result.firstCause();
    }

    public String moduleClassName() {
        return moduleClassName;
    }

    public List<String> failedLibraries() {
        return failedLibraries;
    }

    public Throwable cause() {
        return cause;
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
