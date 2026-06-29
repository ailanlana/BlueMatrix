package io.fntlv.bluematrix.core.module.registration.outcome;

import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationResult;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.logging.BlueLogger;

public final class RegistrationIssueReporter {
    private final BlueLogger logger;

    public RegistrationIssueReporter(BlueLogger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("logger cannot be null");
        }
        this.logger = logger;
    }

    public void registerSuccess(ModuleCandidate module) {
        logger.info("Successfully register module: {} ({}) - {}",
                module.name(), module.id(), module.description()
        );
    }

    public void skip(ModuleCandidate module, String reason) {
        logger.warn(
                "Skipping module: {} ({}) - {}",
                module.name(),
                module.id(),
                reason
        );
    }

    public void skipModule(String moduleName, String moduleId, String reason) {
        logger.warn("Skipping module: {} ({}) - {}", moduleName, moduleId, reason);
    }

    public void skipJarModule(String jarFileName, String moduleClassName, String moduleId, String reason) {
        logger.warn(
                "Skipping module in jar: {} -> {} ({}) - {}",
                jarFileName,
                moduleClassName,
                moduleId,
                reason
        );
    }

    public void registrationResult(ModuleRegistrationResult result) {
        if (result.contexts().isEmpty()) {
            logger.info("Registered modules: none");
        } else {
            logger.info("Registered modules:");
            for (ModuleContext context : result.contexts()) {
                logger.info(" - {} ({}) - {}", context.name(), context.id(), context.getDescriptor().description());
            }
        }

        if (result.issues().isEmpty()) {
            logger.info("Registration issues: none");
            return;
        }

        logger.info("Registration issues:");
        for (ModuleRegistrationIssue issue : result.issues().all()) {
            logger.info(" - {} | {} ({}) - {}",
                    issue.type(),
                    issue.moduleName(),
                    issue.moduleId(),
                    issue.message());
        }
    }
}
