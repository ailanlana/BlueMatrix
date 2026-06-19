package io.fntlv.bluematrix.core.module.registration.instance;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

public interface ModuleInstanceFactory {

    Module create(ModuleCandidate candidate);
}
