package io.fntlv.bluematrix.core.module.lifecycle;

import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.ModuleConditionOutcome;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleDisableException;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleEnableException;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleLoadException;
import io.fntlv.bluematrix.logging.BlueLogger;

final class LifecycleOutcomeHandler {
    private final ModuleEventBus eventBus;
    private final BlueLogger logger;

    LifecycleOutcomeHandler(ModuleEventBus eventBus, BlueLogger logger) {
        this.eventBus = eventBus;
        this.logger = logger;
    }

    boolean handleLoadPreError(ModuleContext context, ModuleLoadEvent.Pre preEvent) {
        if (!preEvent.hasError()) {
            return false;
        }
        String moduleId = context.id();
        ModuleLoadException exception = new ModuleLoadException(moduleId, preEvent.getErrorCause());
        context.markError();
        logger.error(String.format(
                "Module load pre failed: [module=%s], [source=%s], [reason=%s]",
                moduleId,
                preEvent.getErrorSource(),
                preEvent.getErrorMessage()
        ), exception);
        eventBus.publish(new ModuleLoadEvent.Failed(context, exception));
        return true;
    }

    void runLoad(ModuleContext context, LifecycleAction action) {
        String moduleId = context.id();
        try {
            logger.info("Start loading module: [module={}]", moduleId);
            action.run();
            context.markLoaded();
            logger.info("Module loaded success: [module={}]", moduleId);
            eventBus.publish(new ModuleLoadEvent.Post(context));
        } catch (Exception e) {
            ModuleLoadException exception = new ModuleLoadException(moduleId, e);
            context.markError();
            logger.error(String.format("Module load failed: [module=%s]", moduleId), exception);
            eventBus.publish(new ModuleLoadEvent.Failed(context, exception));
        }
    }

    boolean handleEnablePreOutcome(ModuleContext context, ModuleEnableEvent.Pre preEvent) {
        if (preEvent.hasError()) {
            String moduleId = context.id();
            ModuleEnableException exception = new ModuleEnableException(moduleId, preEvent.getErrorCause());
            context.markError();
            logger.error(String.format(
                    "Module enable pre failed: [module=%s], [source=%s], [reason=%s]",
                    moduleId,
                    preEvent.getErrorSource(),
                    preEvent.getErrorMessage()
            ), exception);
            eventBus.publish(new ModuleEnableEvent.Failed(context, exception));
            return true;
        }
        if (preEvent.isCancelled()) {
            skipEnable(context, preEvent.getCancelOutcome());
            return true;
        }
        return false;
    }

    void skipEnable(ModuleContext context, ModuleConditionOutcome outcome) {
        context.markEnableSkipped(outcome);
        logger.warn("Module enable skipped: [module={}], [source={}], [reason={}]",
                context.id(),
                outcome.getSource(),
                outcome.getMessage());
        eventBus.publish(new ModuleEnableEvent.Skipped(context, outcome));
    }

    void runEnable(ModuleContext context, LifecycleAction action) {
        String moduleId = context.id();
        try {
            logger.info("Start enable module: [module={}]", moduleId);
            action.run();
            context.markEnabled();
            logger.info("Module enable success: [module={}]", moduleId);
            eventBus.publish(new ModuleEnableEvent.Post(context));
        } catch (Exception e) {
            ModuleEnableException exception = new ModuleEnableException(moduleId, e);
            context.markError();
            logger.error(String.format("Module enable failed: [module=%s]", moduleId), exception);
            eventBus.publish(new ModuleEnableEvent.Failed(context, exception));
        }
    }

    void runDisable(ModuleContext context, LifecycleAction action) {
        String moduleId = context.id();
        try {
            logger.info("Start disable module: [module={}]", moduleId);
            action.run();
            context.markDisabled();
            logger.info("Module disable success: [module={}]", moduleId);
            eventBus.publish(new ModuleDisableEvent.Post(context));
        } catch (Exception e) {
            ModuleDisableException exception = new ModuleDisableException(moduleId, e);
            context.markError();
            logger.error(String.format("Module disable failed: [module=%s]", moduleId), exception);
            eventBus.publish(new ModuleDisableEvent.Failed(context, exception));
        }
    }

    interface LifecycleAction {
        void run() throws Exception;
    }
}
