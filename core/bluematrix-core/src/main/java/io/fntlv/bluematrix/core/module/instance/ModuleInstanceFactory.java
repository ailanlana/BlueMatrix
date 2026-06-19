package io.fntlv.bluematrix.core.module.instance;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

public interface ModuleInstanceFactory {

    Module create(ModuleCandidate candidate);

    Module createModule(ModuleCandidate candidate);

    <T> T createOther(Class<T> type, OtherInjectionContext context);
}
