package io.fntlv.bluematrix.core.module.lifecycle;

import io.fntlv.bluematrix.core.module.ModuleContext;

public interface LifecycleManager {
    void loadModule(ModuleContext context);
    void enableModule(ModuleContext context);
    void disableModule(ModuleContext context);
    void reloadModule(ModuleContext context);
}
