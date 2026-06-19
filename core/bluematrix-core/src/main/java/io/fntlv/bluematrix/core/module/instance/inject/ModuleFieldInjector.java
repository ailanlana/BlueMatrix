package io.fntlv.bluematrix.core.module.instance.inject;

import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ModuleFieldInjector {
    private final ModuleParameterResolverRegistry parameterResolvers;

    public ModuleFieldInjector(ModuleParameterResolverRegistry parameterResolvers) {
        if (parameterResolvers == null) {
            throw new IllegalArgumentException("parameterResolvers cannot be null");
        }
        this.parameterResolvers = parameterResolvers;
    }

    public void inject(Object target, InjectContext context) {
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        Class<?> type = target.getClass();
        while (type != null && !Object.class.equals(type)) {
            injectDeclaredFields(target, context, type);
            type = type.getSuperclass();
        }
    }

    private void injectDeclaredFields(Object target, InjectContext context, Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (!field.isAnnotationPresent(ModuleInject.class)) {
                continue;
            }
            injectField(target, context, field);
        }
    }

    private void injectField(Object target, InjectContext context, Field field) {
        validateField(field);
        if (!parameterResolvers.supports(field.getType(), context)) {
            throw new ModuleFieldInjectionException("Unsupported module injection field: "
                    + field.getDeclaringClass().getName() + "#" + field.getName()
                    + " (" + field.getType().getName() + ")");
        }
        try {
            field.setAccessible(true);
            field.set(target, parameterResolvers.resolve(field.getType(), context));
        } catch (ModuleFieldInjectionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ModuleFieldInjectionException("Failed to resolve module injection field: "
                    + field.getDeclaringClass().getName() + "#" + field.getName(), e);
        } catch (ReflectiveOperationException e) {
            throw new ModuleFieldInjectionException("Failed to inject module field: "
                    + field.getDeclaringClass().getName() + "#" + field.getName(), e);
        }
    }

    private void validateField(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            throw new ModuleFieldInjectionException("@ModuleInject field cannot be static: "
                    + field.getDeclaringClass().getName() + "#" + field.getName());
        }
        if (Modifier.isFinal(modifiers)) {
            throw new ModuleFieldInjectionException("@ModuleInject field cannot be final: "
                    + field.getDeclaringClass().getName() + "#" + field.getName());
        }
    }
}
