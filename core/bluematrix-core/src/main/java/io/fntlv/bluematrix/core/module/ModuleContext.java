package io.fntlv.bluematrix.core.module;

import lombok.Getter;
import org.reflections.Reflections;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

@Getter
public class ModuleContext {
    private final Module instance;
    private final Class<? extends Module> moduleClass;
    private final ModuleDescriptor descriptor;
    private final Reflections reflections;
    private ModuleConditionOutcome enableConditionOutcome;
    private ModuleState moduleState;

    public boolean isEnableSkipped() {
        return !this.enableConditionOutcome.isMatch();
    }

    public boolean isEnabled(){
        return this.moduleState == ModuleState.ENABLED;
    }

    public boolean canEnable() {
        return this.moduleState == ModuleState.LOADED && this.enableConditionOutcome.isMatch();
    }

    public boolean isDisable() {
        return this.moduleState == ModuleState.DISABLED;
    }

    public boolean isError() {
        return this.moduleState == ModuleState.ERROR;
    }

    public ModuleContext(Module instance, ModuleInfo info) {
        this(instance, ModuleDescriptor.from(instance.getClass(), info));
    }

    public ModuleContext(Module instance, ModuleCandidate candidate) {
        this(instance, candidate.getModuleClass(), candidate.getDescriptor());
    }

    public ModuleContext(Module instance, ModuleDescriptor descriptor) {
        this(instance, instance.getClass(), descriptor);
    }

    public ModuleContext(Module instance, Class<? extends Module> moduleClass, ModuleDescriptor descriptor) {
        this(instance, moduleClass, descriptor, ModuleReflectionsFactory.create(moduleClass, descriptor));
    }

    public ModuleContext(Module instance,
                         Class<? extends Module> moduleClass,
                         ModuleDescriptor descriptor,
                         Reflections reflections) {
        this.instance = instance;
        this.moduleClass = moduleClass;
        this.descriptor = descriptor;
        this.reflections = reflections;
        this.enableConditionOutcome = ModuleConditionOutcome.match();
        this.moduleState = ModuleState.REGISTERED;
    }

    public String id() {
        return descriptor.id();
    }

    public String name() {
        return descriptor.name();
    }

    public boolean enableByDefault() {
        return descriptor.enableByDefault();
    }

    public void markEnableSkipped(ModuleConditionOutcome outcome) {
        if (outcome == null) {
            throw new IllegalArgumentException("outcome cannot be null");
        }
        if (outcome.isMatch()) {
            throw new IllegalArgumentException("markEnableSkipped requires a no-match outcome");
        }
        this.enableConditionOutcome = outcome;
    }

    public void markLoaded() {
        this.moduleState = ModuleState.LOADED;
    }

    public void markEnabled() {
        this.moduleState = ModuleState.ENABLED;
    }

    public void markDisabled() {
        this.moduleState = ModuleState.DISABLED;
    }

    public void markError() {
        this.moduleState = ModuleState.ERROR;
    }

    public enum ModuleState {
        REGISTERED,
        LOADED,
        ENABLED,
        DISABLED,
        ERROR
    }

}
