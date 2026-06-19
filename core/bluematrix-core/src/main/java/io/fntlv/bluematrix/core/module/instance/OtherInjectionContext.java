package io.fntlv.bluematrix.core.module.instance;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import lombok.Getter;
import org.reflections.Reflections;

@Getter
public final class OtherInjectionContext implements InjectContext {
    private final Module moduleInstance;
    private final Class<? extends Module> moduleClass;
    private final ModuleInfo moduleInfo;
    private final Reflections reflections;

    private OtherInjectionContext(Module moduleInstance, ModuleInfo moduleInfo, Reflections reflections) {
        if (moduleInstance == null) {
            throw new IllegalArgumentException("moduleInstance cannot be null");
        }
        if (moduleInfo == null) {
            throw new IllegalArgumentException("moduleInfo cannot be null");
        }
        if (reflections == null) {
            throw new IllegalArgumentException("reflections cannot be null");
        }
        this.moduleInstance = moduleInstance;
        this.moduleClass = moduleInstance.getClass();
        this.moduleInfo = moduleInfo;
        this.reflections = reflections;
    }

    public static OtherInjectionContext from(ModuleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        return new OtherInjectionContext(context.getInstance(), context.getInfo(), context.getReflections());
    }
}
