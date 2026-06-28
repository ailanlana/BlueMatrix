package io.fntlv.bluematrix.core.module.instance.parameter;

import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.storage.DefaultModuleRegistry;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleResolverCompositionTest {
    @TempDir
    File tempDir;

    @Test
    void containerCompositionResolvesRegistryEventBusAndInstanceFactory() {
        ModuleRegistry registry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        ModuleEventBus eventBus = new DefaultModuleEventBus();

        ModuleResolverComposition composition =
                ModuleResolverComposition.forContainer(registry, eventBus, java.util.Collections.emptyList());

        ModuleParameterResolverRegistry resolvers = composition.resolvers();
        ModuleInjectionContext context = moduleContext();

        assertSame(registry, resolvers.resolve(ModuleRegistry.class, context));
        assertSame(eventBus, resolvers.resolve(ModuleEventBus.class, context));
        assertSame(composition.instanceFactory(), resolvers.resolve(ModuleInstanceFactory.class, context));
    }

    @Test
    void containerCompositionRegistersUserResolversIfAbsent() {
        ModuleRegistry registry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        ModuleEventBus eventBus = new DefaultModuleEventBus();

        ModuleResolverComposition composition = ModuleResolverComposition.forContainer(
                registry,
                eventBus,
                Arrays.asList(new StringResolver("first"), new StringResolver("second"))
        );

        assertEquals("first", composition.resolvers().resolve(String.class, moduleContext()));
        assertEquals(1, composition.resolvers().resolvers().stream()
                .filter(StringResolver.class::isInstance)
                .count());
    }

    @Test
    void fromUsesExistingResolversAndRegistersMatchingInstanceFactoryResolver() {
        ModuleParameterResolverRegistry resolvers = new ModuleParameterResolverRegistry();

        ModuleResolverComposition composition = ModuleResolverComposition.from(resolvers);

        assertSame(resolvers, composition.resolvers());
        assertSame(composition.instanceFactory(),
                resolvers.resolve(ModuleInstanceFactory.class, moduleContext()));
    }

    @Test
    void fromKeepsExistingResolversAvailableToInstanceFactory() {
        ModuleParameterResolverRegistry resolvers = new ModuleParameterResolverRegistry();
        resolvers.register(new StringResolver("value"));
        ModuleResolverComposition composition = ModuleResolverComposition.from(resolvers);

        StringConstructorModule module = (StringConstructorModule) composition.instanceFactory()
                .createModule(candidate(StringConstructorModule.class));

        assertEquals("value", module.value);
    }

    private static ModuleInjectionContext moduleContext() {
        return ModuleInjectionContext.from(candidate(TestModule.class));
    }

    private static ModuleCandidate candidate(Class<? extends Module> moduleClass) {
        return new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    private static final class StringResolver implements ModuleParameterResolver {
        private final String value;

        private StringResolver(String value) {
            this.value = value;
        }

        @Override
        public boolean supports(Class<?> parameterType, InjectContext context) {
            return String.class.equals(parameterType) && context instanceof ModuleInjectionContext;
        }

        @Override
        public Object resolve(Class<?> parameterType, InjectContext context) {
            return value;
        }
    }

    @ModuleInfo(id = "test-module", name = "Test Module")
    private static class TestModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "string-constructor-module", name = "String Constructor Module")
    private static class StringConstructorModule implements Module {
        private final String value;

        private StringConstructorModule(String value) {
            this.value = value;
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }
}
