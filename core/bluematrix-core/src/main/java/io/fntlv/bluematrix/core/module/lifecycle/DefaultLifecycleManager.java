package io.fntlv.bluematrix.core.module.lifecycle;

import io.fntlv.bluematrix.core.module.ModuleContext.ModuleState;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleConditionOutcome;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;

public class DefaultLifecycleManager implements LifecycleManager {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(DefaultLifecycleManager.class);

    private final ModuleStore moduleStore;
    private final ModuleEventBus eventBus;
    private final LifecycleOutcomeHandler outcomes;

    public DefaultLifecycleManager(ModuleStore moduleStore, ModuleEventBus eventBus) {
        this.moduleStore = moduleStore;
        this.eventBus = eventBus;
        this.outcomes = new LifecycleOutcomeHandler(eventBus, LOGGER);
    }

    @Override
    public void loadModule(ModuleContext context) {
        String moduleID = context.id();
        Module module = context.getInstance();

        if (context.isError()) {
            LOGGER.warn("Module not load due to previous error: [module={}]", moduleID);
            return;
        }

        ModuleLoadEvent.Pre preEvent = new ModuleLoadEvent.Pre(context);
        eventBus.publish(preEvent);
        if (outcomes.handleLoadPreError(context, preEvent)) {
            return;
        }

        outcomes.runLoad(context, module::onLoad);
    }

    @Override
    public void enableModule(ModuleContext context) {
        String moduleID = context.id();
        Module module = context.getInstance();

        if (context.isError()) {
            LOGGER.warn("Module not enable due to load error: [module={}]", moduleID);
            return;
        }

        if (context.getModuleState() != ModuleState.LOADED) {
            return;
        }

        for (String dependency : context.getDescriptor().dependencies()) {
            boolean dependencyEnabled = this.moduleStore.findById(dependency)
                    .map(ModuleContext::isEnabled)
                    .orElse(false);
            if (!dependencyEnabled) {
                outcomes.skipEnable(context, ModuleConditionOutcome.noMatch(
                        "dependency",
                        "Dependency is not enabled: " + dependency
                ));
                return;
            }
        }

        if (context.isEnableSkipped()) {
            outcomes.skipEnable(context, context.getEnableConditionOutcome());
            return;
        }

        ModuleEnableEvent.Pre preEvent = new ModuleEnableEvent.Pre(context);
        eventBus.publish(preEvent);
        if (outcomes.handleEnablePreOutcome(context, preEvent)) {
            return;
        }

        outcomes.runEnable(context, module::onEnable);
    }

    @Override
    public void disableModule(ModuleContext context) {
        Module module = context.getInstance();

        if (!context.isEnabled()) {
            return;
        }

        eventBus.publish(new ModuleDisableEvent.Pre(context));
        outcomes.runDisable(context, module::onDisable);
    }

    @Override
    public void reloadModule(ModuleContext context) {
        // TODO: implement reload
    }

}
