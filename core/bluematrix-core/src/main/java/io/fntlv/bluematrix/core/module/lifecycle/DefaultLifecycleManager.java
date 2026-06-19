package io.fntlv.bluematrix.core.module.lifecycle;

import io.fntlv.bluematrix.core.module.ModuleContext.ModuleState;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleDisableException;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleEnableException;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleLoadException;
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

    public DefaultLifecycleManager(ModuleStore moduleStore, ModuleEventBus eventBus) {
        this.moduleStore = moduleStore;
        this.eventBus = eventBus;
    }

    @Override
    public void loadModule(ModuleContext context) {
        String moduleID = context.getInfo().id();
        Module module = context.getInstance();

        if (context.isError()) {
            LOGGER.warn("Module not load due to previous error: [module={}]", moduleID);
            return;
        }

        ModuleLoadEvent.Pre preEvent = new ModuleLoadEvent.Pre(context);
        eventBus.publish(preEvent);
        if (preEvent.hasError()) {
            ModuleLoadException exception = new ModuleLoadException(moduleID, preEvent.getErrorCause());
            context.markError();
            LOGGER.error(String.format(
                    "Module load pre failed: [module=%s], [source=%s], [reason=%s]",
                    moduleID,
                    preEvent.getErrorSource(),
                    preEvent.getErrorMessage()
            ), exception);
            eventBus.publish(new ModuleLoadEvent.Failed(context, exception));
            return;
        }

        try {
            LOGGER.info("Start loading module: [module={}]", moduleID);
            module.onLoad();
            context.markLoaded();
            LOGGER.info("Module loaded success: [module={}]", moduleID);
            eventBus.publish(new ModuleLoadEvent.Post(context));
        } catch (Exception e) {
            ModuleLoadException exception = new ModuleLoadException(moduleID, e);
            context.markError();
            LOGGER.error(String.format("Module load failed: [module=%s]", moduleID), exception);
            eventBus.publish(new ModuleLoadEvent.Failed(context, exception));
        }
    }

    @Override
    public void enableModule(ModuleContext context) {
        String moduleID = context.getInfo().id();
        Module module = context.getInstance();

        if (context.isError()) {
            LOGGER.warn("Module not enable due to load error: [module={}]", moduleID);
            return;
        }

        if (context.getModuleState() != ModuleState.LOADED) {
            return;
        }

        for (String dependency : context.getInfo().dependencies()) {
            boolean dependencyEnabled = this.moduleStore.findById(dependency)
                    .map(ModuleContext::isEnabled)
                    .orElse(false);
            if (!dependencyEnabled) {
                context.markEnableSkipped(ModuleConditionOutcome.noMatch(
                        "dependency",
                        "Dependency is not enabled: " + dependency
                ));
                break;
            }
        }

        if (context.isEnableSkipped()) {
            publishEnableSkipped(moduleID, context, context.getEnableConditionOutcome());
            return;
        }

        ModuleEnableEvent.Pre preEvent = new ModuleEnableEvent.Pre(context);
        eventBus.publish(preEvent);
        if (preEvent.hasError()) {
            ModuleEnableException exception = new ModuleEnableException(moduleID, preEvent.getErrorCause());
            context.markError();
            LOGGER.error(String.format(
                    "Module enable pre failed: [module=%s], [source=%s], [reason=%s]",
                    moduleID,
                    preEvent.getErrorSource(),
                    preEvent.getErrorMessage()
            ), exception);
            eventBus.publish(new ModuleEnableEvent.Failed(context, exception));
            return;
        }
        if (preEvent.isCancelled()) {
            ModuleConditionOutcome outcome = preEvent.getCancelOutcome();
            context.markEnableSkipped(outcome);
            publishEnableSkipped(moduleID, context, outcome);
            return;
        }

        try {
            LOGGER.info("Start enable module: [module={}]", moduleID);
            module.onEnable();
            context.markEnabled();
            LOGGER.info("Module enable success: [module={}]", moduleID);
            eventBus.publish(new ModuleEnableEvent.Post(context));
        } catch (Exception e) {
            ModuleEnableException exception = new ModuleEnableException(moduleID, e);
            context.markError();
            LOGGER.error(String.format("Module enable failed: [module=%s]", moduleID), exception);
            eventBus.publish(new ModuleEnableEvent.Failed(context, exception));
        }
    }

    private void publishEnableSkipped(String moduleID, ModuleContext context, ModuleConditionOutcome outcome) {
        LOGGER.warn("Module enable skipped: [module={}], [source={}], [reason={}]",
                moduleID,
                outcome.getSource(),
                outcome.getMessage());
        eventBus.publish(new ModuleEnableEvent.Skipped(context, outcome));
    }

    @Override
    public void disableModule(ModuleContext context) {
        String moduleID = context.getInfo().id();
        Module module = context.getInstance();

        if (!context.isEnabled()) {
            return;
        }

        eventBus.publish(new ModuleDisableEvent.Pre(context));

        try {
            LOGGER.info("Start disable module: [module={}]", moduleID);
            module.onDisable();
            context.markDisabled();
            LOGGER.info("Module disable success: [module={}]", moduleID);
            eventBus.publish(new ModuleDisableEvent.Post(context));
        } catch (Exception e) {
            ModuleDisableException exception = new ModuleDisableException(moduleID, e);
            context.markError();
            LOGGER.error(String.format("Module disable failed: [module=%s]", moduleID), exception);
            eventBus.publish(new ModuleDisableEvent.Failed(context, exception));
        }
    }

    @Override
    public void reloadModule(ModuleContext context) {
        // TODO: implement reload
    }

}
