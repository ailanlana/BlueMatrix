package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.module.ModuleContext;

import java.util.List;

public interface ModuleRegistrar {

    List<ModuleContext> register();
}
