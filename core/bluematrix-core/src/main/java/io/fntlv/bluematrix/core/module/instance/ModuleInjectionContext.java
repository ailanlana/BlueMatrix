package io.fntlv.bluematrix.core.module.instance;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleDescriptor;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import lombok.Getter;

@Getter
public final class ModuleInjectionContext implements InjectContext {
    private final Class<? extends Module> moduleClass;
    private final ModuleDescriptor descriptor;

    private ModuleInjectionContext(Class<? extends Module> moduleClass, ModuleDescriptor descriptor) {
        if (moduleClass == null) {
            throw new IllegalArgumentException("moduleClass cannot be null");
        }
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        this.moduleClass = moduleClass;
        this.descriptor = descriptor;
    }

    public static ModuleInjectionContext from(ModuleCandidate candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate cannot be null");
        }
        return new ModuleInjectionContext(candidate.getModuleClass(), candidate.getDescriptor());
    }
}
