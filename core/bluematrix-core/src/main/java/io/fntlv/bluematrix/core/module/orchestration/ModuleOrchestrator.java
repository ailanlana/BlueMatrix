package io.fntlv.bluematrix.core.module.orchestration;

public interface ModuleOrchestrator {

    void initialize();

    void loadModules();

    void enableModules();

    void disableModules();
}
