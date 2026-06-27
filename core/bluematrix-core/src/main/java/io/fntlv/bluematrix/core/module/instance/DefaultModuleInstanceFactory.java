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

public class DefaultModuleInstanceFactory implements ModuleInstanceFactory {
    private final ModuleInstantiationPolicy instantiationPolicy;
    private final ModuleFieldInjector fieldInjector;

    public DefaultModuleInstanceFactory() {
        this(ModuleParameterResolverRegistry.createDefault());
    }

    public DefaultModuleInstanceFactory(ModuleParameterResolverRegistry parameterResolvers) {
        if (parameterResolvers == null) {
            throw new IllegalArgumentException("parameterResolvers cannot be null");
        }
        this.instantiationPolicy = new ModuleInstantiationPolicy(parameterResolvers);
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
            T instance = constructor.newInstance(resolveParameters(type, context, constructor));
            fieldInjector.inject(instance, context);
            return instance;
        } catch (ModuleInstantiationException e) {
            throw e;
        } catch (ModuleParameterResolutionException e) {
            throw new ModuleInstantiationException(context.id(), e);
        } catch (ModuleFieldInjectionException e) {
            throw new ModuleInstantiationException(context.id(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw instantiationException(context, cause == null ? e : cause);
        } catch (Exception e) {
            throw new ModuleInstantiationException(context.id(), e);
        }
    }

    private ModuleInstantiationException instantiationException(InjectContext context, Throwable cause) {
        if (cause instanceof ModuleInstantiationException) {
            return (ModuleInstantiationException) cause;
        }
        return new ModuleInstantiationException(context.id(), cause);
    }

    private <T> Constructor<T> findConstructor(Class<T> type, InjectContext context) {
        return instantiationPolicy.selectConstructor(type, context);
    }

    private Object[] resolveParameters(Class<?> type, InjectContext context, Constructor<?> constructor) {
        return instantiationPolicy.resolveParameters(type, context, constructor);
    }
}
