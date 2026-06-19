package io.fntlv.bluematrix.core.module.instance;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.instance.inject.ModuleFieldInjectionException;
import io.fntlv.bluematrix.core.module.instance.inject.ModuleFieldInjector;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolutionException;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;

public class DefaultModuleInstanceFactory implements ModuleInstanceFactory {
    private final ModuleParameterResolverRegistry parameterResolvers;
    private final ModuleFieldInjector fieldInjector;

    public DefaultModuleInstanceFactory() {
        this(ModuleParameterResolverRegistry.createDefault());
    }

    public DefaultModuleInstanceFactory(ModuleParameterResolverRegistry parameterResolvers) {
        if (parameterResolvers == null) {
            throw new IllegalArgumentException("parameterResolvers cannot be null");
        }
        this.parameterResolvers = parameterResolvers;
        this.fieldInjector = new ModuleFieldInjector(parameterResolvers);
    }

    @Override
    public Module create(ModuleCandidate candidate) {
        return createModule(candidate);
    }

    @Override
    public Module createModule(ModuleCandidate candidate) {
        return create(candidate.getModuleClass(), ModuleInjectionContext.from(candidate));
    }

    @Override
    public <T> T createOther(Class<T> type, OtherInjectionContext context) {
        return create(type, context);
    }

    private <T> T create(Class<T> type, InjectContext context) {
        try {
            Constructor<T> constructor = findConstructor(type, context);
            constructor.setAccessible(true);
            T instance = constructor.newInstance(resolveParameters(context, constructor));
            fieldInjector.inject(instance, context);
            return instance;
        } catch (ModuleInstantiationException e) {
            throw e;
        } catch (ModuleParameterResolutionException e) {
            throw new ModuleInstantiationException(context.getModuleInfo().id(), e);
        } catch (ModuleFieldInjectionException e) {
            throw new ModuleInstantiationException(context.getModuleInfo().id(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw instantiationException(context, cause == null ? e : cause);
        } catch (Exception e) {
            throw new ModuleInstantiationException(context.getModuleInfo().id(), e);
        }
    }

    private ModuleInstantiationException instantiationException(InjectContext context, Throwable cause) {
        if (cause instanceof ModuleInstantiationException) {
            return (ModuleInstantiationException) cause;
        }
        return new ModuleInstantiationException(context.getModuleInfo().id(), cause);
    }

    @SuppressWarnings("unchecked")
    private <T> Constructor<T> findConstructor(Class<T> type, InjectContext context) {
        return (Constructor<T>) Arrays.stream(type.getDeclaredConstructors())
                .filter(constructor -> supportsAllParameters(constructor, context))
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new ModuleParameterResolutionException(
                        "No resolvable constructor found for module instance: " + type.getName()
                ));
    }

    private boolean supportsAllParameters(Constructor<?> constructor, InjectContext context) {
        for (Class<?> parameterType : constructor.getParameterTypes()) {
            if (!parameterResolvers.supports(parameterType, context)) {
                return false;
            }
        }
        return true;
    }

    private Object[] resolveParameters(InjectContext context, Constructor<?> constructor) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = parameterResolvers.resolve(parameterTypes[i], context);
        }
        return parameters;
    }
}
