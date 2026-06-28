package io.fntlv.bluematrix.core.bootstrap;

import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.orchestration.ModuleOrchestrator;

public final class BlueMatrixContainerRuntime {
    private final ModuleRegistry registry;
    private final ModuleOrchestrator moduleOrchestrator;
    private final ModuleEventBus eventBus;
    private final ModuleParameterResolverRegistry parameterResolvers;
    private final ModuleInstanceFactory instanceFactory;

    BlueMatrixContainerRuntime(ModuleRegistry registry,
                               ModuleOrchestrator moduleOrchestrator,
                               ModuleEventBus eventBus,
                               ModuleParameterResolverRegistry parameterResolvers,
                               ModuleInstanceFactory instanceFactory) {
        this.registry = registry;
        this.moduleOrchestrator = moduleOrchestrator;
        this.eventBus = eventBus;
        this.parameterResolvers = parameterResolvers;
        this.instanceFactory = instanceFactory;
    }

    public ModuleRegistry registry() {
        return registry;
    }

    public ModuleOrchestrator moduleOrchestrator() {
        return moduleOrchestrator;
    }

    public ModuleEventBus eventBus() {
        return eventBus;
    }

    public ModuleParameterResolverRegistry parameterResolvers() {
        return parameterResolvers;
    }

    public ModuleInstanceFactory instanceFactory() {
        return instanceFactory;
    }
}
