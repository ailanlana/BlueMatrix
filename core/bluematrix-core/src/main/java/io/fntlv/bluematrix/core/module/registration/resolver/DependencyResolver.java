package io.fntlv.bluematrix.core.module.registration.resolver;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationStageResult;

import java.util.List;

public interface DependencyResolver {

    /**
     * Resolves the load order of modules.
     *
     * @param modules modules discovered from module providers
     * @return modules that can be registered, sorted by dependencies and load order
     */
    List<ModuleCandidate> resolve(List<ModuleCandidate> modules);

    default ModuleRegistrationStageResult<ModuleCandidate> resolveWithResult(List<ModuleCandidate> modules) {
        return ModuleRegistrationStageResult.of(resolve(modules));
    }
}
