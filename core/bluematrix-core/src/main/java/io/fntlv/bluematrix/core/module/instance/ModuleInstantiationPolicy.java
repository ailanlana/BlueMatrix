package io.fntlv.bluematrix.core.module.instance;

import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolutionException;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class ModuleInstantiationPolicy {
    private final ModuleParameterResolverRegistry parameterResolvers;

    ModuleInstantiationPolicy(ModuleParameterResolverRegistry parameterResolvers) {
        if (parameterResolvers == null) {
            throw new IllegalArgumentException("parameterResolvers cannot be null");
        }
        this.parameterResolvers = parameterResolvers;
    }

    @SuppressWarnings("unchecked")
    <T> Constructor<T> selectConstructor(Class<T> type, InjectContext context) {
        List<Constructor<?>> resolvableConstructors = new ArrayList<>();
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (supportsAllParameters(constructor, context)) {
                resolvableConstructors.add(constructor);
            }
        }
        if (resolvableConstructors.isEmpty()) {
            throw new ModuleParameterResolutionException("No resolvable constructor found for module instance: "
                    + type.getName() + "; unsupported constructors: " + describeConstructors(type, context));
        }

        int maxParameterCount = resolvableConstructors.stream()
                .map(Constructor::getParameterCount)
                .max(Comparator.naturalOrder())
                .orElse(0);
        List<Constructor<?>> bestConstructors = resolvableConstructors.stream()
                .filter(constructor -> constructor.getParameterCount() == maxParameterCount)
                .collect(Collectors.toList());
        if (bestConstructors.size() > 1) {
            throw new ModuleParameterResolutionException("Ambiguous resolvable constructors for module instance: "
                    + type.getName() + "; candidates: " + describe(bestConstructors));
        }
        return (Constructor<T>) bestConstructors.get(0);
    }

    Object[] resolveParameters(Class<?> type, InjectContext context, Constructor<?> constructor) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            try {
                parameters[i] = parameterResolvers.resolve(parameterTypes[i], context);
            } catch (ModuleParameterResolutionException e) {
                throw parameterException(type, constructor, i, parameterTypes[i], e);
            } catch (RuntimeException e) {
                throw parameterException(type, constructor, i, parameterTypes[i], e);
            }
        }
        return parameters;
    }

    private ModuleParameterResolutionException parameterException(
            Class<?> type,
            Constructor<?> constructor,
            int index,
            Class<?> parameterType,
            RuntimeException cause
    ) {
        return new ModuleParameterResolutionException("Failed to resolve constructor parameter: "
                + type.getName() + "#<init>" + signature(constructor)
                + " parameter[" + index + "] " + parameterType.getName(), cause);
    }

    private boolean supportsAllParameters(Constructor<?> constructor, InjectContext context) {
        for (Class<?> parameterType : constructor.getParameterTypes()) {
            if (!parameterResolvers.supports(parameterType, context)) {
                return false;
            }
        }
        return true;
    }

    private String describeConstructors(Class<?> type, InjectContext context) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 0) {
            return "[]";
        }
        List<String> descriptions = new ArrayList<>();
        for (Constructor<?> constructor : constructors) {
            descriptions.add(signatureWithUnsupportedParameters(constructor, context));
        }
        return descriptions.toString();
    }

    private String signatureWithUnsupportedParameters(Constructor<?> constructor, InjectContext context) {
        List<String> unsupportedParameters = new ArrayList<>();
        for (Class<?> parameterType : constructor.getParameterTypes()) {
            if (!parameterResolvers.supports(parameterType, context)) {
                unsupportedParameters.add(parameterType.getName());
            }
        }
        return signature(constructor) + " unsupported=" + unsupportedParameters;
    }

    private String describe(List<Constructor<?>> constructors) {
        return constructors.stream()
                .map(this::signature)
                .collect(Collectors.toList())
                .toString();
    }

    private String signature(Constructor<?> constructor) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        List<String> names = new ArrayList<>(parameterTypes.length);
        for (Class<?> parameterType : parameterTypes) {
            names.add(parameterType.getName());
        }
        return "(" + String.join(", ", names) + ")";
    }
}
