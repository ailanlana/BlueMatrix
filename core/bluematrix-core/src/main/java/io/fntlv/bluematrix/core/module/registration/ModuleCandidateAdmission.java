package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.module.registration.outcome.RegistrationOutcomeClassifier;
import io.fntlv.bluematrix.core.module.registration.outcome.RegistrationOutcomeCollector;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class ModuleCandidateAdmission {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(ModuleCandidateAdmission.class);

    private final RegistrationOutcomeClassifier outcomes;

    ModuleCandidateAdmission() {
        this(new RegistrationOutcomeClassifier(LOGGER));
    }

    ModuleCandidateAdmission(RegistrationOutcomeClassifier outcomes) {
        if (outcomes == null) {
            throw new IllegalArgumentException("outcomes cannot be null");
        }
        this.outcomes = outcomes;
    }

    ModuleRegistrationStageResult<ModuleCandidate> admit(List<ModuleCandidate> candidates) {
        if (candidates == null) {
            throw new IllegalArgumentException("candidates cannot be null");
        }
        Set<String> conflicts = findConflictIds(candidates);
        RegistrationOutcomeCollector<ModuleCandidate> collector = new RegistrationOutcomeCollector<>();
        for (ModuleCandidate candidate : candidates) {
            if (conflicts.contains(candidate.id())) {
                collector.issue(outcomes.duplicateModuleId(candidate));
            } else {
                collector.pass(candidate);
            }
        }
        return collector.toStageResult();
    }

    private Set<String> findConflictIds(List<ModuleCandidate> candidates) {
        Map<String, Long> countById = candidates.stream()
                .collect(Collectors.groupingBy(ModuleCandidate::id, LinkedHashMap::new, Collectors.counting()));
        return countById.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
