package io.fntlv.bluematrix.core.module.instance.parameter;

import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleInstanceParameterResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleParameterResolverRegistry {
    private final List<ModuleParameterResolver> resolvers = new ArrayList<>();

    public static ModuleParameterResolverRegistry createDefault() {
        ModuleParameterResolverRegistry registry = new ModuleParameterResolverRegistry();
        registry.register(new ModuleInstanceParameterResolver());
        return registry;
    }

    public void register(ModuleParameterResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("resolver cannot be null");
        }
        resolvers.add(resolver);
    }

    public boolean registerIfAbsent(ModuleParameterResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("resolver cannot be null");
        }
        for (ModuleParameterResolver existing : resolvers) {
            if (existing.getClass().equals(resolver.getClass())) {
                return false;
            }
        }
        resolvers.add(resolver);
        return true;
    }

    public boolean supports(Class<?> parameterType, InjectContext context) {
        return findResolver(parameterType, context) != null;
    }

    public Object resolve(Class<?> parameterType, InjectContext context) {
        ModuleParameterResolver resolver = findResolver(parameterType, context);
        if (resolver == null) {
            throw new ModuleParameterResolutionException("Unsupported module parameter: " + parameterType.getName());
        }
        return resolver.resolve(parameterType, context);
    }

    List<ModuleParameterResolver> matchingResolvers(Class<?> parameterType, InjectContext context) {
        List<ModuleParameterResolver> matchingResolvers = new ArrayList<>();
        for (ModuleParameterResolver resolver : resolvers) {
            if (resolver.supports(parameterType, context)) {
                matchingResolvers.add(resolver);
            }
        }
        return Collections.unmodifiableList(matchingResolvers);
    }

    public List<ModuleParameterResolver> resolvers() {
        return Collections.unmodifiableList(resolvers);
    }

    private ModuleParameterResolver findResolver(Class<?> parameterType, InjectContext context) {
        List<ModuleParameterResolver> matchingResolvers = matchingResolvers(parameterType, context);
        if (matchingResolvers.isEmpty()) {
            return null;
        }
        return matchingResolvers.get(0);
    }
}
