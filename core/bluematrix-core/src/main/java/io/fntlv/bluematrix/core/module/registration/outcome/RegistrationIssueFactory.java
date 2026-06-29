package io.fntlv.bluematrix.core.module.registration.outcome;

import io.fntlv.bluematrix.core.module.ModuleDescriptor;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.issue.issues.CircularDependencyIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.DuplicateModuleIdIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.InstantiationFailedIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.MissingRequiredDependencyIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.RuntimeLibraryLoadFailedIssue;
import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoadResult;

import java.util.List;

public final class RegistrationIssueFactory {
    public DuplicateModuleIdIssue duplicateModuleId(ModuleCandidate module) {
        return new DuplicateModuleIdIssue(module, duplicateModuleIdReason(module));
    }

    public InstantiationFailedIssue instantiationFailed(ModuleCandidate module, ModuleInstantiationException cause) {
        return new InstantiationFailedIssue(module, instantiationFailedReason(cause), cause);
    }

    public MissingRequiredDependencyIssue missingRequiredDependency(ModuleCandidate module, List<String> missingDependencies) {
        return new MissingRequiredDependencyIssue(module, missingRequiredDependencyReason(missingDependencies), missingDependencies);
    }

    public CircularDependencyIssue circularDependency(ModuleCandidate module, List<String> circularModuleIds) {
        return new CircularDependencyIssue(module, circularDependencyReason(circularModuleIds), circularModuleIds);
    }

    public RuntimeLibraryLoadFailedIssue runtimeLibraryFailed(ModuleDescriptor descriptor,
                                                             Class<?> moduleClass,
                                                             ModuleRuntimeLibraryLoadResult result) {
        return new RuntimeLibraryLoadFailedIssue(
                descriptor,
                moduleClass,
                result,
                runtimeLibraryFailedReason(result)
        );
    }

    public RuntimeLibraryLoadFailedIssue runtimeLibraryFailed(String moduleId,
                                                             String moduleName,
                                                             String moduleClassName,
                                                             ModuleRuntimeLibraryLoadResult result) {
        return new RuntimeLibraryLoadFailedIssue(
                moduleId,
                moduleName,
                moduleClassName,
                result,
                runtimeLibraryFailedReason(result)
        );
    }

    String duplicateModuleIdReason(ModuleCandidate module) {
        return "Duplicate module id: " + module.id();
    }

    String instantiationFailedReason(ModuleInstantiationException cause) {
        return "Failed to instantiate module: " + cause.getMessage();
    }

    String missingRequiredDependencyReason(List<String> missingDependencies) {
        return "Missing required dependencies: " + String.join(", ", missingDependencies);
    }

    String circularDependencyReason(List<String> circularModuleIds) {
        return "Circular dependency detected with modules: " + String.join(", ", circularModuleIds);
    }

    String runtimeLibraryFailedReason(ModuleRuntimeLibraryLoadResult result) {
        return "Failed to load runtime libraries: " + result.failureSummary();
    }
}
