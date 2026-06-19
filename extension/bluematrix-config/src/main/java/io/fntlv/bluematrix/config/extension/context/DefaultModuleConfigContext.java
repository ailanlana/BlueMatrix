package io.fntlv.bluematrix.config.extension.context;

import io.fntlv.bluematrix.core.module.Module;

public class DefaultModuleConfigContext implements ModuleConfigContext {
    private final String moduleId;
    private volatile ModuleConfigState state;

    public DefaultModuleConfigContext(String moduleId) {
        if (moduleId == null) {
            throw new IllegalArgumentException("moduleId cannot be null");
        }
        this.moduleId = moduleId;
    }

    @Override
    public <T> T get(Class<T> type) {
        return requireState().get(type);
    }

    @Override
    public String moduleId() {
        return moduleId;
    }

    @Override
    public Module module() {
        return requireState().module();
    }

    public synchronized void bindState(ModuleConfigState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        if (this.state != null && this.state != state) {
            throw new IllegalStateException("Module config context is already loaded: " + moduleId);
        }
        if (!moduleId.equals(state.moduleId())) {
            throw new IllegalStateException("Module config state module id does not match context: " + moduleId);
        }
        this.state = state;
    }

    public ModuleConfigState state() {
        return requireState();
    }

    private ModuleConfigState requireState() {
        ModuleConfigState current = state;
        if (current == null) {
            throw new IllegalStateException("Module config context is not loaded yet: " + moduleId);
        }
        return current;
    }
}
