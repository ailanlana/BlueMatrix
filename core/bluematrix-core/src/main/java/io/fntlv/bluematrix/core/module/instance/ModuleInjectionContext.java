package io.fntlv.bluematrix.core.module.instance;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import lombok.Getter;
import org.reflections.Reflections;

@Getter
public final class ModuleInjectionContext implements InjectContext {
    private final Class<? extends Module> moduleClass;
    private final ModuleInfo moduleInfo;
    private final Reflections reflections;

    private ModuleInjectionContext(Class<? extends Module> moduleClass, ModuleInfo moduleInfo, Reflections reflections) {
        if (moduleClass == null) {
            throw new IllegalArgumentException("moduleClass cannot be null");
        }
        if (moduleInfo == null) {
            throw new IllegalArgumentException("moduleInfo cannot be null");
        }
        if (reflections == null) {
            throw new IllegalArgumentException("reflections cannot be null");
        }
        this.moduleClass = moduleClass;
        this.moduleInfo = moduleInfo;
        this.reflections = reflections;
    }

    public static ModuleInjectionContext from(ModuleCandidate candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate cannot be null");
        }
        return new ModuleInjectionContext(candidate.getModuleClass(), candidate.getModuleInfo(), candidate.getReflections());
    }
}
