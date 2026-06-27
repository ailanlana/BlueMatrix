package io.fntlv.bluematrix.core.module.instance;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleDescriptor;

public interface InjectContext {

    Class<? extends Module> getModuleClass();

    ModuleDescriptor getDescriptor();

    default String id() {
        return getDescriptor().id();
    }
}
