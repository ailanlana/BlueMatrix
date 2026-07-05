package io.fntlv.bluematrix.config.extension.context;

import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityContext;
import io.fntlv.bluematrix.core.module.Module;

public final class ModuleConfigContext implements ModuleCapabilityContext {
    private final String moduleId;
    private final ModuleConfigState state;

    public ModuleConfigContext(String moduleId, ModuleConfigState state) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        this.moduleId = moduleId;
        this.state = state;
    }

    public <T> T get(Class<T> type) {
        return state.get(type);
    }

    @Override
    public String moduleId() {
        return moduleId;
    }

    public Module module() {
        return state.module();
    }
}
