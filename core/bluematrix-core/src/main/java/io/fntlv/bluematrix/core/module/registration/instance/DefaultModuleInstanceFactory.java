package io.fntlv.bluematrix.core.module.registration.instance;

import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.instance.inject.ModuleFieldInjectionException;
import io.fntlv.bluematrix.core.module.registration.instance.inject.ModuleFieldInjector;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolutionException;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolverRegistry;

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
        try {
            Constructor<? extends Module> constructor = findConstructor(candidate);
            constructor.setAccessible(true);
            Module module = constructor.newInstance(resolveParameters(candidate, constructor));
            fieldInjector.inject(module, candidate);
            return module;
        } catch (ModuleInstantiationException e) {
            throw e;
        } catch (ModuleParameterResolutionException e) {
            throw new ModuleInstantiationException(candidate.getModuleInfo().id(), e);
        } catch (ModuleFieldInjectionException e) {
            throw new ModuleInstantiationException(candidate.getModuleInfo().id(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw instantiationException(candidate, cause == null ? e : cause);
        } catch (Exception e) {
            throw new ModuleInstantiationException(candidate.getModuleInfo().id(), e);
        }
    }

    private ModuleInstantiationException instantiationException(ModuleCandidate candidate, Throwable cause) {
        if (cause instanceof ModuleInstantiationException) {
            return (ModuleInstantiationException) cause;
        }
        return new ModuleInstantiationException(candidate.getModuleInfo().id(), cause);
    }

    @SuppressWarnings("unchecked")
    private Constructor<? extends Module> findConstructor(ModuleCandidate candidate) {
        return (Constructor<? extends Module>) Arrays.stream(candidate.getModuleClass().getDeclaredConstructors())
                .filter(this::supportsAllParameters)
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new ModuleParameterResolutionException(
                        "No resolvable constructor found for module: " + candidate.getModuleClass().getName()
                ));
    }

    private boolean supportsAllParameters(Constructor<?> constructor) {
        for (Class<?> parameterType : constructor.getParameterTypes()) {
            if (!parameterResolvers.supports(parameterType)) {
                return false;
            }
        }
        return true;
    }

    private Object[] resolveParameters(ModuleCandidate candidate,
                                       Constructor<? extends Module> constructor) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = parameterResolvers.resolve(parameterTypes[i], candidate);
        }
        return parameters;
    }
}
