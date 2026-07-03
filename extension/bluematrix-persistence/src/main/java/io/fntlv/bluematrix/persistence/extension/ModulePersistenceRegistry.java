package io.fntlv.bluematrix.persistence.extension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ModulePersistenceRegistry {
    private final Map<String, DefaultModulePersistenceContext> contexts =
            Collections.synchronizedMap(new HashMap<>());

    void register(String moduleId, DefaultModulePersistenceContext context) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        contexts.put(moduleId, context);
    }

    boolean contains(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }
        return contexts.containsKey(moduleId);
    }

    Optional<ModulePersistenceContext> find(String moduleId) {
        return findInternal(moduleId).map(context -> context);
    }

    Optional<DefaultModulePersistenceContext> findInternal(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(contexts.get(moduleId));
    }

    DefaultModulePersistenceContext get(String moduleId) {
        return findInternal(moduleId).orElseThrow(() -> new IllegalStateException(missingContextMessage(moduleId)));
    }

    void remove(String moduleId) {
        contexts.remove(moduleId);
    }

    private String missingContextMessage(String moduleId) {
        return "ModulePersistenceContext should be registered for persistence-enabled modules. "
                + "Missing context indicates an unexpected persistence extension state: " + moduleId;
    }
}
