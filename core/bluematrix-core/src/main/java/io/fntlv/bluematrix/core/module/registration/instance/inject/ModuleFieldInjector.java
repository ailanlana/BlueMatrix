package io.fntlv.bluematrix.core.module.registration.instance.inject;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolverRegistry;

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

    public void inject(Module module, ModuleCandidate candidate) {
        if (module == null) {
            throw new IllegalArgumentException("module cannot be null");
        }
        if (candidate == null) {
            throw new IllegalArgumentException("candidate cannot be null");
        }

        Class<?> type = module.getClass();
        while (type != null && Module.class.isAssignableFrom(type)) {
            injectDeclaredFields(module, candidate, type);
            type = type.getSuperclass();
        }
    }

    private void injectDeclaredFields(Module module, ModuleCandidate candidate, Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (!field.isAnnotationPresent(ModuleInject.class)) {
                continue;
            }
            injectField(module, candidate, field);
        }
    }

    private void injectField(Module module, ModuleCandidate candidate, Field field) {
        validateField(field);
        if (!parameterResolvers.supports(field.getType())) {
            throw new ModuleFieldInjectionException("Unsupported module injection field: "
                    + field.getDeclaringClass().getName() + "#" + field.getName()
                    + " (" + field.getType().getName() + ")");
        }
        try {
            field.setAccessible(true);
            field.set(module, parameterResolvers.resolve(field.getType(), candidate));
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
