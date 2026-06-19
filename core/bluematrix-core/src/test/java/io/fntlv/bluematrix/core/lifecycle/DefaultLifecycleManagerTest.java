package io.fntlv.bluematrix.core.module.lifecycle;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.ModuleContext.ModuleState;
import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleDisableException;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleEnableException;
import io.fntlv.bluematrix.core.module.lifecycle.exception.ModuleLoadException;
import io.fntlv.bluematrix.core.module.ModuleConditionOutcome;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultLifecycleManagerTest {

    @Test
    void lifecycleEventsArePublishedDirectly() {
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(listener);
        ModuleContext context = context(new TestModule());

        lifecycleManager.loadModule(context);
        lifecycleManager.enableModule(context);
        lifecycleManager.disableModule(context);

        assertEquals(1, listener.loadPreCount);
        assertEquals(1, listener.loadPostCount);
        assertEquals(1, listener.enablePreCount);
        assertEquals(1, listener.enablePostCount);
        assertEquals(1, listener.disablePreCount);
        assertEquals(1, listener.disablePostCount);
    }

    @Test
    void enableEventsDoNotRunWhenModuleCannotEnable() {
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(listener);

        lifecycleManager.enableModule(context(new TestModule()));

        assertEquals(0, listener.enablePreCount);
        assertEquals(0, listener.enablePostCount);
        assertEquals(0, listener.enableSkippedCount);
    }

    @Test
    void failingListenerDoesNotBlockFollowingListeners() {
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(new ThrowingLoadPreListener(), listener);

        lifecycleManager.loadModule(context(new TestModule()));

        assertEquals(1, listener.loadPreCount);
    }

    @Test
    void failingLoadPreListenerDoesNotBlockModuleLoad() {
        LoadTrackingModule module = new LoadTrackingModule();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(new ThrowingLoadPreListener());
        ModuleContext context = context(module);

        lifecycleManager.loadModule(context);

        assertEquals(1, module.loadCount);
        assertEquals(ModuleState.LOADED, context.getModuleState());
    }

    @Test
    void errorModuleDoesNotLoad() {
        LoadTrackingModule module = new LoadTrackingModule();
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(listener);
        ModuleContext context = context(module);
        context.markError();

        lifecycleManager.loadModule(context);

        assertEquals(0, module.loadCount);
        assertEquals(0, listener.loadPreCount);
        assertEquals(0, listener.loadPostCount);
        assertEquals(ModuleState.ERROR, context.getModuleState());
    }

    @Test
    void enablePreListenerCanCancelEnable() {
        EnableTrackingModule module = new EnableTrackingModule();
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(new CancelEnablePreListener(), listener);
        ModuleContext context = context(module);

        lifecycleManager.loadModule(context);
        lifecycleManager.enableModule(context);

        assertEquals(ModuleState.LOADED, context.getModuleState());
        assertEquals(1, module.loadCount);
        assertEquals(0, module.enableCount);
        assertEquals(1, listener.enableSkippedCount);
        assertEquals("test", context.getEnableConditionOutcome().getSource());
        assertEquals("Skipped by test listener", context.getEnableConditionOutcome().getMessage());
    }

    @Test
    void enablePreCancelRequiresNoMatchOutcome() {
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(new TestModule()));

        assertThrows(IllegalArgumentException.class, () -> event.cancel(null));
        assertThrows(IllegalArgumentException.class, () -> event.cancel(ModuleConditionOutcome.match()));
    }

    @Test
    void enablePreErrorRequiresValidArguments() {
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(new TestModule()));
        IllegalStateException cause = new IllegalStateException("broken");

        assertThrows(IllegalArgumentException.class, () -> event.error(null, "message", cause));
        assertThrows(IllegalArgumentException.class, () -> event.error(" ", "message", cause));
        assertThrows(IllegalArgumentException.class, () -> event.error("test", null, cause));
        assertThrows(IllegalArgumentException.class, () -> event.error("test", " ", cause));
        assertThrows(IllegalArgumentException.class, () -> event.error("test", "message", null));
    }

    @Test
    void loadPreErrorRequiresValidArguments() {
        ModuleLoadEvent.Pre event = new ModuleLoadEvent.Pre(context(new TestModule()));
        IllegalStateException cause = new IllegalStateException("broken");

        assertThrows(IllegalArgumentException.class, () -> event.error(null, "message", cause));
        assertThrows(IllegalArgumentException.class, () -> event.error(" ", "message", cause));
        assertThrows(IllegalArgumentException.class, () -> event.error("test", null, cause));
        assertThrows(IllegalArgumentException.class, () -> event.error("test", " ", cause));
        assertThrows(IllegalArgumentException.class, () -> event.error("test", "message", null));
    }

    @Test
    void loadPreErrorSkipsModuleLoadAndPublishesFailed() {
        LoadTrackingModule module = new LoadTrackingModule();
        LoadPreErrorListener errorListener = new LoadPreErrorListener();
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(errorListener, listener);
        ModuleContext context = context(module);

        lifecycleManager.loadModule(context);

        assertEquals(0, module.loadCount);
        assertEquals(ModuleState.ERROR, context.getModuleState());
        assertEquals(1, listener.loadPreCount);
        assertEquals(0, listener.loadPostCount);
        ModuleLoadException exception = assertInstanceOf(ModuleLoadException.class, listener.loadFailedCause);
        assertEquals("load-tracking-module", exception.getModuleId());
        assertSame(errorListener.failure, exception.getCause());
    }

    @Test
    void enablePreErrorSkipsModuleEnableAndPublishesFailed() {
        EnableTrackingModule module = new EnableTrackingModule();
        EnablePreErrorListener errorListener = new EnablePreErrorListener();
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(errorListener, listener);
        ModuleContext context = context(module);

        lifecycleManager.loadModule(context);
        lifecycleManager.enableModule(context);

        assertEquals(1, module.loadCount);
        assertEquals(0, module.enableCount);
        assertEquals(ModuleState.ERROR, context.getModuleState());
        assertEquals(1, listener.enablePreCount);
        assertEquals(0, listener.enablePostCount);
        assertEquals(0, listener.enableSkippedCount);
        ModuleEnableException exception = assertInstanceOf(ModuleEnableException.class, listener.enableFailedCause);
        assertEquals("enable-tracking-module", exception.getModuleId());
        assertSame(errorListener.failure, exception.getCause());
    }

    @Test
    void dependencyNoMatchSkipsEnableWithoutChangingLoadedState() {
        ModuleStore moduleStore = new ModuleStore();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(moduleStore);
        DependentModule module = new DependentModule();
        ModuleContext context = context(module);

        lifecycleManager.loadModule(context);
        lifecycleManager.enableModule(context);

        assertEquals(ModuleState.LOADED, context.getModuleState());
        assertFalse(module.enabled);
        assertEquals("dependency", context.getEnableConditionOutcome().getSource());
        assertEquals("Dependency is not enabled: dependency-module", context.getEnableConditionOutcome().getMessage());
    }

    @Test
    void dependencyNoMatchPublishesEnableSkippedEventOnly() {
        ModuleStore moduleStore = new ModuleStore();
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(moduleStore, listener);
        ModuleContext context = context(new DependentModule());

        lifecycleManager.loadModule(context);
        lifecycleManager.enableModule(context);

        assertEquals(1, listener.enableSkippedCount);
        assertEquals(0, listener.enablePreCount);
        assertEquals(0, listener.enablePostCount);
    }

    @Test
    void loadFailurePublishesModuleLoadException() {
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(listener);
        FailingLoadModule module = new FailingLoadModule();
        ModuleContext context = context(module);

        lifecycleManager.loadModule(context);

        assertEquals(ModuleState.ERROR, context.getModuleState());
        ModuleLoadException exception = assertInstanceOf(ModuleLoadException.class, listener.loadFailedCause);
        assertEquals("failing-load-module", exception.getModuleId());
        assertSame(module.failure, exception.getCause());
    }

    @Test
    void enableFailurePublishesModuleEnableException() {
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(listener);
        FailingEnableModule module = new FailingEnableModule();
        ModuleContext context = context(module);

        lifecycleManager.loadModule(context);
        lifecycleManager.enableModule(context);

        assertEquals(ModuleState.ERROR, context.getModuleState());
        ModuleEnableException exception = assertInstanceOf(ModuleEnableException.class, listener.enableFailedCause);
        assertEquals("failing-enable-module", exception.getModuleId());
        assertSame(module.failure, exception.getCause());
    }

    @Test
    void disableFailurePublishesModuleDisableException() {
        CountingListener listener = new CountingListener();
        DefaultLifecycleManager lifecycleManager = lifecycleManager(listener);
        FailingDisableModule module = new FailingDisableModule();
        ModuleContext context = context(module);

        lifecycleManager.loadModule(context);
        lifecycleManager.enableModule(context);
        lifecycleManager.disableModule(context);

        assertEquals(ModuleState.ERROR, context.getModuleState());
        ModuleDisableException exception = assertInstanceOf(ModuleDisableException.class, listener.disableFailedCause);
        assertEquals("failing-disable-module", exception.getModuleId());
        assertSame(module.failure, exception.getCause());
    }

    private static DefaultLifecycleManager lifecycleManager(Object... listeners) {
        return lifecycleManager(new ModuleStore(), listeners);
    }

    private static DefaultLifecycleManager lifecycleManager(ModuleStore moduleStore, Object... listeners) {
        ModuleEventBus eventBus = new DefaultModuleEventBus();
        for (Object listener : listeners) {
            eventBus.registerListener(listener);
        }
        return new DefaultLifecycleManager(moduleStore, eventBus);
    }

    private static ModuleContext context(Module module) {
        return new ModuleContext(module, module.getClass().getAnnotation(ModuleInfo.class));
    }

    @ModuleInfo(id = "test-module", name = "Test Module")
    private static class TestModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "dependent-module", name = "Dependent Module", dependencies = "dependency-module")
    private static class DependentModule implements Module {
        private boolean enabled;

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
            enabled = true;
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "failing-load-module", name = "Failing Load Module")
    private static class FailingLoadModule implements Module {
        private final IllegalStateException failure = new IllegalStateException("load failed");

        @Override
        public void onLoad() {
            throw failure;
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "failing-enable-module", name = "Failing Enable Module")
    private static class FailingEnableModule implements Module {
        private final IllegalStateException failure = new IllegalStateException("enable failed");

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
            throw failure;
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "failing-disable-module", name = "Failing Disable Module")
    private static class FailingDisableModule implements Module {
        private final IllegalStateException failure = new IllegalStateException("disable failed");

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
            throw failure;
        }
    }

    private static class CountingListener {
        int loadPreCount;
        int loadPostCount;
        Throwable loadFailedCause;
        int enablePreCount;
        int enablePostCount;
        int enableSkippedCount;
        Throwable enableFailedCause;
        int disablePreCount;
        int disablePostCount;
        Throwable disableFailedCause;

        @ModuleEventListener
        public void onLoadPre(ModuleLoadEvent.Pre event) {
            loadPreCount++;
        }

        @ModuleEventListener
        public void onLoadPost(ModuleLoadEvent.Post event) {
            loadPostCount++;
        }

        @ModuleEventListener
        public void onLoadFailed(ModuleLoadEvent.Failed event) {
            loadFailedCause = event.getCause();
        }

        @ModuleEventListener
        public void onEnablePre(ModuleEnableEvent.Pre event) {
            enablePreCount++;
        }

        @ModuleEventListener
        public void onEnablePost(ModuleEnableEvent.Post event) {
            enablePostCount++;
        }

        @ModuleEventListener
        public void onEnableSkipped(ModuleEnableEvent.Skipped event) {
            enableSkippedCount++;
        }

        @ModuleEventListener
        public void onEnableFailed(ModuleEnableEvent.Failed event) {
            enableFailedCause = event.getCause();
        }

        @ModuleEventListener
        public void onDisablePre(ModuleDisableEvent.Pre event) {
            disablePreCount++;
        }

        @ModuleEventListener
        public void onDisablePost(ModuleDisableEvent.Post event) {
            disablePostCount++;
        }

        @ModuleEventListener
        public void onDisableFailed(ModuleDisableEvent.Failed event) {
            disableFailedCause = event.getCause();
        }
    }

    private static class ThrowingLoadPreListener {
        @ModuleEventListener
        public void onLoadPre(ModuleLoadEvent.Pre event) {
            throw new IllegalStateException("boom");
        }
    }

    private static class CancelEnablePreListener {
        @ModuleEventListener
        public void onEnablePre(ModuleEnableEvent.Pre event) {
            event.cancel(ModuleConditionOutcome.noMatch(
                    "test",
                    "Skipped by test listener"
            ));
        }
    }

    private static class LoadPreErrorListener {
        private final IllegalStateException failure = new IllegalStateException("pre failed");

        @ModuleEventListener
        public void onLoadPre(ModuleLoadEvent.Pre event) {
            event.error("test", "Load pre failed", failure);
        }
    }

    private static class EnablePreErrorListener {
        private final IllegalStateException failure = new IllegalStateException("pre failed");

        @ModuleEventListener
        public void onEnablePre(ModuleEnableEvent.Pre event) {
            event.error("test", "Enable pre failed", failure);
        }
    }

    @ModuleInfo(id = "load-tracking-module", name = "Load Tracking Module")
    private static class LoadTrackingModule implements Module {
        private int loadCount;

        @Override
        public void onLoad() {
            loadCount++;
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "enable-tracking-module", name = "Enable Tracking Module")
    private static class EnableTrackingModule implements Module {
        private int loadCount;
        private int enableCount;

        @Override
        public void onLoad() {
            loadCount++;
        }

        @Override
        public void onEnable() {
            enableCount++;
        }

        @Override
        public void onDisable() {
        }
    }
}
