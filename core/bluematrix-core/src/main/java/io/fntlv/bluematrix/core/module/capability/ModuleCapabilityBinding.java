package io.fntlv.bluematrix.core.module.capability;

public final class ModuleCapabilityBinding<C extends ModuleCapabilityContext, S extends ModuleCapabilityState> {
    private final String moduleId;
    private final C context;
    private final S state;

    ModuleCapabilityBinding(String moduleId, C context, S state) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        this.moduleId = moduleId;
        this.context = context;
        this.state = state;
    }

    public String moduleId() {
        return moduleId;
    }

    public C context() {
        return context;
    }

    public S state() {
        return state;
    }
}
