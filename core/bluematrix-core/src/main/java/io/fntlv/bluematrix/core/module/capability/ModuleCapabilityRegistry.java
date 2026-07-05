package io.fntlv.bluematrix.core.module.capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ModuleCapabilityRegistry {
    private final List<ModuleCapability<?, ?>> capabilities = new ArrayList<>();

    public <C extends ModuleCapabilityContext, S extends ModuleCapabilityState>
    ModuleCapability<C, S> register(ModuleCapability<C, S> capability) {
        if (capability == null) {
            throw new IllegalArgumentException("capability cannot be null");
        }
        if (findById(capability.id()).isPresent()) {
            throw new IllegalArgumentException("Duplicate module capability: " + capability.id());
        }
        capabilities.add(capability);
        return capability;
    }

    public List<ModuleCapability<?, ?>> capabilities() {
        return Collections.unmodifiableList(capabilities);
    }

    public Optional<ModuleCapability<?, ?>> findById(String capabilityId) {
        if (capabilityId == null || capabilityId.trim().isEmpty()) {
            return Optional.empty();
        }
        for (ModuleCapability<?, ?> capability : capabilities) {
            if (capability.id().equals(capabilityId)) {
                return Optional.of(capability);
            }
        }
        return Optional.empty();
    }

    public List<ModuleCapability<?, ?>> findByContextType(Class<?> contextType) {
        if (contextType == null) {
            return Collections.emptyList();
        }
        List<ModuleCapability<?, ?>> matches = new ArrayList<>();
        for (ModuleCapability<?, ?> capability : capabilities) {
            if (capability.supports(contextType)) {
                matches.add(capability);
            }
        }
        return Collections.unmodifiableList(matches);
    }
}
