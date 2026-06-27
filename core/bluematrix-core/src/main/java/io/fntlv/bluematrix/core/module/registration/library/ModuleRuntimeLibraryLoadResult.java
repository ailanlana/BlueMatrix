package io.fntlv.bluematrix.core.module.registration.library;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class ModuleRuntimeLibraryLoadResult {
    private final String moduleId;
    private final List<ModuleRuntimeLibraryFailure> failures;

    private ModuleRuntimeLibraryLoadResult(String moduleId, List<ModuleRuntimeLibraryFailure> failures) {
        this.moduleId = moduleId;
        this.failures = immutableCopy(failures);
    }

    public static ModuleRuntimeLibraryLoadResult success(String moduleId) {
        return new ModuleRuntimeLibraryLoadResult(moduleId, Collections.emptyList());
    }

    public static ModuleRuntimeLibraryLoadResult of(String moduleId, List<ModuleRuntimeLibraryFailure> failures) {
        return new ModuleRuntimeLibraryLoadResult(moduleId, failures);
    }

    public String moduleId() {
        return moduleId;
    }

    public boolean success() {
        return failures.isEmpty();
    }

    public boolean failed() {
        return !success();
    }

    public List<ModuleRuntimeLibraryFailure> failures() {
        return failures;
    }

    public List<String> failedLibraries() {
        List<String> libraries = new ArrayList<>();
        for (ModuleRuntimeLibraryFailure failure : failures) {
            if (failure.library() != null && !failure.library().trim().isEmpty()) {
                libraries.add(failure.library());
            }
        }
        return Collections.unmodifiableList(libraries);
    }

    public String failureSummary() {
        List<String> libraries = failedLibraries();
        if (!libraries.isEmpty()) {
            return String.join(", ", libraries);
        }
        return "runtime library setup";
    }

    public Throwable firstCause() {
        if (failures.isEmpty()) {
            return null;
        }
        return failures.get(0).cause();
    }

    private static List<ModuleRuntimeLibraryFailure> immutableCopy(List<ModuleRuntimeLibraryFailure> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
