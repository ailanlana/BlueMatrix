package io.fntlv.bluematrix.core.event;

import io.fntlv.bluematrix.core.event.ModuleEventException;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DefaultModuleEventBus implements ModuleEventBus {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(DefaultModuleEventBus.class);

    private final List<ListenerMethod> listenerMethods = new ArrayList<>();

    public DefaultModuleEventBus() {
    }

    @Override
    public void registerListener(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(ModuleEventListener.class)) {
                continue;
            }
            Class<?>[] parameterTypes = getClasses(method);
            method.setAccessible(true);
            listenerMethods.add(new ListenerMethod(listener, method, parameterTypes[0]));
        }
    }

    @Nonnull
    private static Class<?>[] getClasses(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new ModuleEventException("@ModuleEventListener method must have exactly one parameter: "
                    + method.getName());
        }
        if (!ModuleEvent.class.isAssignableFrom(parameterTypes[0])) {
            throw new ModuleEventException("@ModuleEventListener method parameter must implement ModuleEvent: "
                    + method.getName());
        }
        return parameterTypes;
    }

    @Override
    public void publish(ModuleEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        for (ListenerMethod listenerMethod : listenerMethods) {
            if (!listenerMethod.supports(event)) {
                continue;
            }
            try {
                listenerMethod.invoke(event);
            } catch (Throwable e) {
                String format = String.format(
                        "Module event listener failed: [event=%s], [listener=%s], [method=%s]",
                        event.getClass().getSimpleName(),
                        listenerMethod.listener.getClass().getSimpleName(),
                        listenerMethod.method.getName()
                );
                ModuleEventException exception = new ModuleEventException(format, e);
                LOGGER.error(format, exception);
            }
        }
    }

    private static final class ListenerMethod {
        private final Object listener;
        private final Method method;
        private final Class<?> eventType;

        private ListenerMethod(Object listener, Method method, Class<?> eventType) {
            this.listener = listener;
            this.method = method;
            this.eventType = eventType;
        }

        private boolean supports(ModuleEvent event) {
            return eventType.isAssignableFrom(event.getClass());
        }

        private void invoke(ModuleEvent event) throws Throwable {
            try {
                method.invoke(listener, event);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw cause == null ? e : cause;
            }
        }
    }
}
