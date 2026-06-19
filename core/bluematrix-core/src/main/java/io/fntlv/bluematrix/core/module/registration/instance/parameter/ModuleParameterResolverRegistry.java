package io.fntlv.bluematrix.core.module.registration.instance.parameter;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleParameterResolverRegistry {
    private final List<ModuleParameterResolver> resolvers = new ArrayList<>();

    public static ModuleParameterResolverRegistry createDefault() {
        return new ModuleParameterResolverRegistry();
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

    public boolean supports(Class<?> parameterType) {
        return findResolver(parameterType) != null;
    }

    public Object resolve(Class<?> parameterType, ModuleCandidate candidate) {
        ModuleParameterResolver resolver = findResolver(parameterType);
        if (resolver == null) {
            throw new ModuleParameterResolutionException("Unsupported module constructor parameter: " + parameterType.getName());
        }
        return resolver.resolve(parameterType, candidate);
    }

    public List<ModuleParameterResolver> resolvers() {
        return Collections.unmodifiableList(resolvers);
    }

    private ModuleParameterResolver findResolver(Class<?> parameterType) {
        for (ModuleParameterResolver resolver : resolvers) {
            if (resolver.supports(parameterType)) {
                return resolver;
            }
        }
        return null;
    }
}
