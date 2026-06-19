package io.fntlv.bluematrix.core.module.instance;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import org.reflections.Reflections;

public interface InjectContext {

    Class<? extends Module> getModuleClass();

    ModuleInfo getModuleInfo();

    Reflections getReflections();
}
