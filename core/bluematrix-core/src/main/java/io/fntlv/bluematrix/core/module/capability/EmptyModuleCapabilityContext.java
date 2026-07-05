package io.fntlv.bluematrix.core.module.capability;

public final class EmptyModuleCapabilityContext implements ModuleCapabilityContext {
    private final String moduleId;

    public EmptyModuleCapabilityContext(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        this.moduleId = moduleId;
    }

    @Override
    public String moduleId() {
        return moduleId;
    }
}
