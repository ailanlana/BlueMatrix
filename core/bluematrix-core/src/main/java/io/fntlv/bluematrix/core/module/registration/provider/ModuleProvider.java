package io.fntlv.bluematrix.core.module.registration.provider;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

import java.util.List;

public interface ModuleProvider {

    /**
     * Discovers and retrieves module candidates from this provider.
     *
     * @return discovered module candidates with metadata needed for registration.
     */
    List<ModuleCandidate> discoverModules();
}
