package io.fntlv.bluematrix.core.event;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.event.ModuleEventException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultModuleEventBusTest {

    @Test
    void annotatedMethodReceivesMatchingEvent() {
        DefaultModuleEventBus eventBus = new DefaultModuleEventBus();
        LoadPreListener listener = new LoadPreListener();
        eventBus.registerListener(listener);

        eventBus.publish(new ModuleLoadEvent.Pre(context()));

        assertEquals(1, listener.count);
    }

    @Test
    void parentEventTypeReceivesChildEvents() {
        DefaultModuleEventBus eventBus = new DefaultModuleEventBus();
        LoadParentListener listener = new LoadParentListener();
        eventBus.registerListener(listener);

        eventBus.publish(new ModuleLoadEvent.Pre(context()));
        eventBus.publish(new ModuleLoadEvent.Post(context()));

        assertEquals(2, listener.count);
    }

    @Test
    void unannotatedMethodDoesNotReceiveEvent() {
        DefaultModuleEventBus eventBus = new DefaultModuleEventBus();
        UnannotatedListener listener = new UnannotatedListener();
        eventBus.registerListener(listener);

        eventBus.publish(new ModuleLoadEvent.Pre(context()));

        assertEquals(0, listener.count);
    }

    @Test
    void listenerMethodMustHaveExactlyOneParameter() {
        DefaultModuleEventBus eventBus = new DefaultModuleEventBus();

        assertThrows(ModuleEventException.class, () -> eventBus.registerListener(new NoParameterListener()));
    }

    @Test
    void listenerMethodParameterMustBeModuleEvent() {
        DefaultModuleEventBus eventBus = new DefaultModuleEventBus();

        assertThrows(ModuleEventException.class, () -> eventBus.registerListener(new InvalidParameterListener()));
    }

    @Test
    void failingListenerDoesNotBlockFollowingListener() {
        DefaultModuleEventBus eventBus = new DefaultModuleEventBus();
        LoadPreListener listener = new LoadPreListener();
        eventBus.registerListener(new ThrowingListener());
        eventBus.registerListener(listener);

        eventBus.publish(new ModuleLoadEvent.Pre(context()));

        assertEquals(1, listener.count);
    }

    @Test
    void listenerErrorDoesNotBlockFollowingListener() {
        DefaultModuleEventBus eventBus = new DefaultModuleEventBus();
        LoadPreListener listener = new LoadPreListener();
        eventBus.registerListener(new ErrorListener());
        eventBus.registerListener(listener);

        eventBus.publish(new ModuleLoadEvent.Pre(context()));

        assertEquals(1, listener.count);
    }

    private static ModuleContext context() {
        TestModule module = new TestModule();
        return new ModuleContext(module, TestModule.class.getAnnotation(ModuleInfo.class));
    }

    private static class LoadPreListener {
        private int count;

        @ModuleEventListener
        public void onLoadPre(ModuleLoadEvent.Pre event) {
            count++;
        }
    }

    private static class LoadParentListener {
        private int count;

        @ModuleEventListener
        public void onLoad(ModuleLoadEvent event) {
            count++;
        }
    }

    private static class UnannotatedListener {
        private int count;

        public void onLoadPre(ModuleLoadEvent.Pre event) {
            count++;
        }
    }

    private static class NoParameterListener {
        @ModuleEventListener
        public void onLoadPre() {
        }
    }

    private static class InvalidParameterListener {
        @ModuleEventListener
        public void onLoadPre(String event) {
        }
    }

    private static class ThrowingListener {
        @ModuleEventListener
        public void onLoadPre(ModuleLoadEvent.Pre event) {
            throw new IllegalStateException("boom");
        }
    }

    private static class ErrorListener {
        @ModuleEventListener
        public void onLoadPre(ModuleLoadEvent.Pre event) {
            throw new AssertionError("boom");
        }
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

}
