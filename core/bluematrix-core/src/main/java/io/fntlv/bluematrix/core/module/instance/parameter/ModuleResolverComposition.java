package io.fntlv.bluematrix.core.module.instance.parameter;

import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleEventBusParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleInstanceFactoryParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleRegistryParameterResolver;

import java.util.Collections;
import java.util.List;

public final class ModuleResolverComposition {
    private final ModuleParameterResolverRegistry resolvers;
    private final ModuleInstanceFactory instanceFactory;

    private ModuleResolverComposition(ModuleParameterResolverRegistry resolvers,
                                      ModuleInstanceFactory instanceFactory) {
        this.resolvers = resolvers;
        this.instanceFactory = instanceFactory;
    }

    public static ModuleResolverComposition forContainer(ModuleRegistry registry,
                                                         ModuleEventBus eventBus,
                                                         List<ModuleParameterResolver> userResolvers) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("eventBus cannot be null");
        }
        ModuleParameterResolverRegistry resolvers = ModuleParameterResolverRegistry.createDefault();
        resolvers.register(new ModuleRegistryParameterResolver(registry));
        resolvers.register(new ModuleEventBusParameterResolver(eventBus));
        registerUserResolvers(resolvers, userResolvers == null ? Collections.emptyList() : userResolvers);
        return from(resolvers);
    }

    public static ModuleResolverComposition from(ModuleParameterResolverRegistry resolvers) {
        if (resolvers == null) {
            throw new IllegalArgumentException("resolvers cannot be null");
        }
        ModuleInstanceFactory instanceFactory = createInstanceFactory(resolvers);
        return new ModuleResolverComposition(resolvers, instanceFactory);
    }

    public ModuleParameterResolverRegistry resolvers() {
        return resolvers;
    }

    public ModuleInstanceFactory instanceFactory() {
        return instanceFactory;
    }

    private static void registerUserResolvers(ModuleParameterResolverRegistry resolvers,
                                              List<ModuleParameterResolver> userResolvers) {
        for (ModuleParameterResolver resolver : userResolvers) {
            resolvers.registerIfAbsent(resolver);
        }
    }

    private static ModuleInstanceFactory createInstanceFactory(ModuleParameterResolverRegistry resolvers) {
        ModuleInstanceFactory instanceFactory = new DefaultModuleInstanceFactory(resolvers);
        resolvers.register(new ModuleInstanceFactoryParameterResolver(instanceFactory));
        return instanceFactory;
    }
}
