package io.fntlv.bluematrix.core.module;

import lombok.Getter;
import org.reflections.Reflections;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

@Getter
public class ModuleContext {
    private final Module instance;
    private final ModuleInfo info;
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
        this(instance, new ModuleCandidate(instance.getClass(), info));
    }

    public ModuleContext(Module instance, ModuleCandidate candidate) {
        this(instance, candidate.getModuleInfo(), candidate.getReflections());
    }

    public ModuleContext(Module instance, ModuleInfo info, Reflections reflections) {
        this.instance = instance;
        this.info = info;
        this.reflections = reflections;
        this.enableConditionOutcome = ModuleConditionOutcome.match();
        this.moduleState = ModuleState.REGISTERED;
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
