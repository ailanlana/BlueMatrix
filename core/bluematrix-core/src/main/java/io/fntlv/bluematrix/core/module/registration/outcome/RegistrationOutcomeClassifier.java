package io.fntlv.bluematrix.core.module.registration.outcome;

import io.fntlv.bluematrix.core.module.ModuleDescriptor;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoadResult;
import io.fntlv.bluematrix.logging.BlueLogger;

import java.util.List;

public final class RegistrationOutcomeClassifier {
    private final RegistrationIssueFactory factory;
    private final RegistrationIssueReporter reporter;

    public RegistrationOutcomeClassifier(BlueLogger logger) {
        this(new RegistrationIssueFactory(), new RegistrationIssueReporter(logger));
    }

    public RegistrationOutcomeClassifier(RegistrationIssueFactory factory, RegistrationIssueReporter reporter) {
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null");
        }
        if (reporter == null) {
            throw new IllegalArgumentException("reporter cannot be null");
        }
        this.factory = factory;
        this.reporter = reporter;
    }

    public ModuleRegistrationIssue duplicateModuleId(ModuleCandidate module) {
        ModuleRegistrationIssue issue = factory.duplicateModuleId(module);
        reporter.skip(module, issue.message());
        return issue;
    }

    public ModuleRegistrationIssue instantiationFailed(ModuleCandidate module, ModuleInstantiationException cause) {
        ModuleRegistrationIssue issue = factory.instantiationFailed(module, cause);
        reporter.skip(module, issue.message());
        return issue;
    }

    public ModuleRegistrationIssue missingRequiredDependency(ModuleCandidate module, List<String> missingDependencies) {
        ModuleRegistrationIssue issue = factory.missingRequiredDependency(module, missingDependencies);
        reporter.skip(module, issue.message());
        return issue;
    }

    public ModuleRegistrationIssue circularDependency(ModuleCandidate module, List<String> circularModuleIds) {
        ModuleRegistrationIssue issue = factory.circularDependency(module, circularModuleIds);
        reporter.skip(module, issue.message());
        return issue;
    }

    public ModuleRegistrationIssue runtimeLibraryFailed(ModuleDescriptor descriptor,
                                                        Class<?> moduleClass,
                                                        ModuleRuntimeLibraryLoadResult result) {
        ModuleRegistrationIssue issue = factory.runtimeLibraryFailed(descriptor, moduleClass, result);
        reporter.skipModule(descriptor.name(), descriptor.id(), issue.message());
        return issue;
    }

    public ModuleRegistrationIssue runtimeLibraryFailedInJar(String jarFileName,
                                                             String moduleId,
                                                             String moduleName,
                                                             String moduleClassName,
                                                             ModuleRuntimeLibraryLoadResult result) {
        ModuleRegistrationIssue issue = factory.runtimeLibraryFailed(moduleId, moduleName, moduleClassName, result);
        reporter.skipJarModule(jarFileName, moduleClassName, moduleId, issue.message());
        return issue;
    }

    public RegistrationIssueReporter reporter() {
        return reporter;
    }
}
