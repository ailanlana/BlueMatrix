package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.event.DefaultModuleEventBus;
import io.fntlv.bluematrix.core.event.ModuleEventBus;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.ModuleRegistry;
import io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.OtherInjectionContext;
import io.fntlv.bluematrix.core.module.instance.inject.ModuleFieldInjectionException;
import io.fntlv.bluematrix.core.module.instance.inject.ModuleInject;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolutionException;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleEventBusParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleInstanceFactoryParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.resolver.ModuleRegistryParameterResolver;
import io.fntlv.bluematrix.core.module.storage.DefaultModuleRegistry;
import io.fntlv.bluematrix.core.module.storage.ModuleStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModuleInstanceFactoryTest {

    @TempDir
    File tempDir;

    private final DefaultModuleInstanceFactory factory = new DefaultModuleInstanceFactory();

    @Test
    void createsModuleWithNoArgumentConstructor() {
        ModuleCandidate candidate = candidate(FactoryModule.class);

        Module module = factory.create(candidate);

        assertEquals(FactoryModule.class, module.getClass());
    }

    @Test
    void wrapsInstantiationFailure() {
        ModuleCandidate candidate = candidate(MissingNoArgumentConstructorModule.class);

        RuntimeException exception = assertThrows(ModuleInstantiationException.class, () -> factory.create(candidate));

        assertInstanceOf(ModuleInstantiationException.class, exception);
        assertInstanceOf(ModuleParameterResolutionException.class, exception.getCause());
    }

    @Test
    void injectsModuleRegistryConstructorParameter() {
        ModuleCandidate candidate = candidate(ModuleRegistryConstructorModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        DefaultModuleInstanceFactory registryFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleRegistryParameterResolver(moduleRegistry)
        ));

        Module module = registryFactory.create(candidate);

        assertSame(moduleRegistry, ((ModuleRegistryConstructorModule) module).moduleRegistry);
    }

    @Test
    void injectsModuleEventBusConstructorParameter() {
        ModuleCandidate candidate = candidate(ModuleEventBusConstructorModule.class);
        ModuleEventBus eventBus = new DefaultModuleEventBus();
        DefaultModuleInstanceFactory eventBusFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleEventBusParameterResolver(eventBus)
        ));

        Module module = eventBusFactory.create(candidate);

        assertSame(eventBus, ((ModuleEventBusConstructorModule) module).eventBus);
    }

    @Test
    void injectsModuleInstanceFactoryConstructorParameter() {
        ModuleCandidate candidate = candidate(ModuleInstanceFactoryConstructorModule.class);
        ModuleParameterResolverRegistry registry = new ModuleParameterResolverRegistry();
        DefaultModuleInstanceFactory instanceFactory = new DefaultModuleInstanceFactory(registry);
        registry.register(new ModuleInstanceFactoryParameterResolver(instanceFactory));

        Module module = instanceFactory.create(candidate);

        assertSame(instanceFactory, ((ModuleInstanceFactoryConstructorModule) module).instanceFactory);
    }

    @Test
    void injectsModuleRegistryField() {
        ModuleCandidate candidate = candidate(ModuleRegistryFieldModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        DefaultModuleInstanceFactory registryFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleRegistryParameterResolver(moduleRegistry)
        ));

        Module module = registryFactory.create(candidate);

        assertSame(moduleRegistry, ((ModuleRegistryFieldModule) module).moduleRegistry);
    }

    @Test
    void injectsPrivateModuleEventBusField() {
        ModuleCandidate candidate = candidate(PrivateEventBusFieldModule.class);
        ModuleEventBus eventBus = new DefaultModuleEventBus();
        DefaultModuleInstanceFactory eventBusFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleEventBusParameterResolver(eventBus)
        ));

        Module module = eventBusFactory.create(candidate);

        assertSame(eventBus, ((PrivateEventBusFieldModule) module).eventBus);
    }

    @Test
    void constructorAndFieldInjectionCanBeUsedTogether() {
        ModuleCandidate candidate = candidate(ConstructorAndFieldModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        ModuleEventBus eventBus = new DefaultModuleEventBus();
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        parameterResolvers.register(new ModuleRegistryParameterResolver(moduleRegistry));
        parameterResolvers.register(new ModuleEventBusParameterResolver(eventBus));
        DefaultModuleInstanceFactory mixedFactory = new DefaultModuleInstanceFactory(parameterResolvers);

        Module module = mixedFactory.create(candidate);
        ConstructorAndFieldModule typedModule = (ConstructorAndFieldModule) module;

        assertSame(moduleRegistry, typedModule.moduleRegistry);
        assertSame(eventBus, typedModule.eventBus);
    }

    @Test
    void choosesConstructorWithMostResolvableParameters() {
        ModuleCandidate candidate = candidate(MoreParametersPreferredModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        DefaultModuleInstanceFactory registryFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleRegistryParameterResolver(moduleRegistry)
        ));

        Module module = registryFactory.create(candidate);

        assertSame(moduleRegistry, ((MoreParametersPreferredModule) module).moduleRegistry);
    }

    @Test
    void skipsConstructorWithUnresolvableParameters() {
        ModuleCandidate candidate = candidate(UnresolvableMoreParametersModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        DefaultModuleInstanceFactory registryFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleRegistryParameterResolver(moduleRegistry)
        ));

        Module module = registryFactory.create(candidate);

        assertSame(moduleRegistry, ((UnresolvableMoreParametersModule) module).moduleRegistry);
        assertNull(((UnresolvableMoreParametersModule) module).value);
    }

    @Test
    void ambiguousResolvableConstructorsFailInstantiation() {
        ModuleCandidate candidate = candidate(AmbiguousConstructorModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        ModuleEventBus eventBus = new DefaultModuleEventBus();
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        parameterResolvers.register(new ModuleRegistryParameterResolver(moduleRegistry));
        parameterResolvers.register(new ModuleEventBusParameterResolver(eventBus));
        DefaultModuleInstanceFactory ambiguousFactory = new DefaultModuleInstanceFactory(parameterResolvers);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> ambiguousFactory.create(candidate));

        assertInstanceOf(ModuleParameterResolutionException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Ambiguous resolvable constructors"));
        assertTrue(exception.getCause().getMessage().contains(AmbiguousConstructorModule.class.getName()));
    }

    @Test
    void fieldsWithoutModuleInjectAreNotInjected() {
        ModuleCandidate candidate = candidate(UnmarkedFieldModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        DefaultModuleInstanceFactory registryFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleRegistryParameterResolver(moduleRegistry)
        ));

        Module module = registryFactory.create(candidate);

        assertNull(((UnmarkedFieldModule) module).moduleRegistry);
    }

    @Test
    void finalInjectionFieldFailsInstantiation() {
        ModuleCandidate candidate = candidate(FinalFieldModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        DefaultModuleInstanceFactory registryFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleRegistryParameterResolver(moduleRegistry)
        ));

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> registryFactory.create(candidate));

        assertInstanceOf(ModuleFieldInjectionException.class, exception.getCause());
    }

    @Test
    void staticInjectionFieldFailsInstantiation() {
        ModuleCandidate candidate = candidate(StaticFieldModule.class);
        ModuleRegistry moduleRegistry = new DefaultModuleRegistry(new ModuleStore(), tempDir);
        DefaultModuleInstanceFactory registryFactory = new DefaultModuleInstanceFactory(parameterResolvers(
                new ModuleRegistryParameterResolver(moduleRegistry)
        ));

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> registryFactory.create(candidate));

        assertInstanceOf(ModuleFieldInjectionException.class, exception.getCause());
    }

    @Test
    void unsupportedInjectionFieldFailsInstantiation() {
        ModuleCandidate candidate = candidate(UnsupportedFieldModule.class);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> factory.create(candidate));

        assertInstanceOf(ModuleFieldInjectionException.class, exception.getCause());
    }

    @Test
    void unsupportedConstructorParameterMessageIncludesTargetAndParameterType() {
        ModuleCandidate candidate = candidate(MissingNoArgumentConstructorModule.class);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> factory.create(candidate));

        assertTrue(exception.getCause().getMessage().contains(MissingNoArgumentConstructorModule.class.getName()));
        assertTrue(exception.getCause().getMessage().contains(String.class.getName()));
    }

    @Test
    void unsupportedFieldMessageIncludesFieldAndType() {
        ModuleCandidate candidate = candidate(UnsupportedFieldModule.class);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> factory.create(candidate));

        assertTrue(exception.getCause().getMessage().contains(UnsupportedFieldModule.class.getName() + "#value"));
        assertTrue(exception.getCause().getMessage().contains(String.class.getName()));
    }

    @Test
    void constructorFailureUsesOriginalCause() {
        ModuleCandidate candidate = candidate(ThrowingConstructorModule.class);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> factory.create(candidate));

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("constructor failed", exception.getCause().getMessage());
    }

    @Test
    void constructorFailureDoesNotExposeInvocationTargetException() {
        ModuleCandidate candidate = candidate(ThrowingConstructorModule.class);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> factory.create(candidate));

        if (exception.getCause() instanceof InvocationTargetException) {
            throw new AssertionError("Constructor failure should expose the original cause");
        }
    }

    @Test
    void constructorModuleInstantiationExceptionIsNotWrappedAgain() {
        ModuleCandidate candidate = candidate(ThrowingModuleInstantiationConstructorModule.class);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> factory.create(candidate));

        assertEquals("Failed to instantiate module: inner", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("inner cause", exception.getCause().getMessage());
    }

    @Test
    void registerIfAbsentSkipsResolverWithSameClass() {
        ModuleParameterResolverRegistry registry = new ModuleParameterResolverRegistry();

        registry.registerIfAbsent(new TestParameterResolver());
        registry.registerIfAbsent(new TestParameterResolver());

        assertEquals(1, registry.resolvers().size());
    }

    @Test
    void createsOtherInstanceWithOtherResolvers() {
        OtherInjectionContext context = otherContext(new FactoryModule());
        ModuleParameterResolverRegistry registry = new ModuleParameterResolverRegistry();
        registry.register(new StringParameterResolver(OtherInjectionContext.class, "other-value"));
        DefaultModuleInstanceFactory instanceFactory = new DefaultModuleInstanceFactory(registry);

        OtherComponent component = instanceFactory.createOther(OtherComponent.class, context);

        assertEquals("other-value", component.value);
    }

    @Test
    void createModuleIgnoresOtherResolvers() {
        ModuleCandidate candidate = candidate(StringConstructorModule.class);
        ModuleParameterResolverRegistry registry = new ModuleParameterResolverRegistry();
        registry.register(new StringParameterResolver(OtherInjectionContext.class, "other-value"));
        DefaultModuleInstanceFactory instanceFactory = new DefaultModuleInstanceFactory(registry);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> instanceFactory.createModule(candidate));

        assertInstanceOf(ModuleParameterResolutionException.class, exception.getCause());
    }

    @Test
    void resolverRegistrationOrderDeterminesConstructorParameterValue() {
        ModuleCandidate candidate = candidate(StringConstructorModule.class);
        ModuleParameterResolverRegistry registry = new ModuleParameterResolverRegistry();
        registry.register(new StringParameterResolver(ModuleInjectionContext.class, "first-value"));
        registry.register(new StringParameterResolver(ModuleInjectionContext.class, "second-value"));
        DefaultModuleInstanceFactory instanceFactory = new DefaultModuleInstanceFactory(registry);

        Module module = instanceFactory.createModule(candidate);

        assertEquals("first-value", ((StringConstructorModule) module).value);
    }

    @Test
    void createsOtherInstanceWithModuleConstructorParameter() {
        FactoryModule module = new FactoryModule();

        ModuleOwnerComponent component = factory.createOther(ModuleOwnerComponent.class, otherContext(module));

        assertSame(module, component.module);
    }

    @Test
    void injectsModuleInstanceIntoOtherField() {
        FactoryModule module = new FactoryModule();

        ModuleOwnerFieldComponent component = factory.createOther(ModuleOwnerFieldComponent.class, otherContext(module));

        assertSame(module, component.module);
    }

    private ModuleParameterResolverRegistry parameterResolvers(ModuleParameterResolver resolver) {
        ModuleParameterResolverRegistry registry = new ModuleParameterResolverRegistry();
        registry.register(resolver);
        return registry;
    }

    private ModuleCandidate candidate(Class<? extends Module> moduleClass) {
        return new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    private OtherInjectionContext otherContext(Module module) {
        return OtherInjectionContext.from(new ModuleContext(module, module.getClass().getAnnotation(ModuleInfo.class)));
    }

    private static class TestParameterResolver implements ModuleParameterResolver {
        @Override
        public boolean supports(Class<?> parameterType, InjectContext context) {
            return false;
        }

        @Override
        public Object resolve(Class<?> parameterType, InjectContext context) {
            return null;
        }
    }

    private static class StringParameterResolver implements ModuleParameterResolver {
        private final Class<? extends InjectContext> contextType;
        private final String value;

        private StringParameterResolver(Class<? extends InjectContext> contextType, String value) {
            this.contextType = contextType;
            this.value = value;
        }

        @Override
        public boolean supports(Class<?> parameterType, InjectContext context) {
            return contextType.isInstance(context) && String.class.equals(parameterType);
        }

        @Override
        public Object resolve(Class<?> parameterType, InjectContext context) {
            return value;
        }
    }

    private static class OtherComponent {
        private final String value;

        private OtherComponent(String value) {
            this.value = value;
        }
    }

    private static class ModuleOwnerComponent {
        private final FactoryModule module;

        private ModuleOwnerComponent(FactoryModule module) {
            this.module = module;
        }
    }

    private static class ModuleOwnerFieldComponent {
        @ModuleInject
        private FactoryModule module;
    }

    @ModuleInfo(id = "factory-module", name = "Factory Module")
    public static class FactoryModule implements Module {
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

    @ModuleInfo(id = "missing-no-argument-constructor", name = "Missing No Argument Constructor")
    public static class MissingNoArgumentConstructorModule implements Module {
        public MissingNoArgumentConstructorModule(String value) {
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

    @ModuleInfo(id = "string-constructor", name = "String Constructor")
    public static class StringConstructorModule implements Module {
        private final String value;

        public StringConstructorModule(String value) {
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

    @ModuleInfo(id = "module-registry-constructor", name = "Module Registry Constructor")
    public static class ModuleRegistryConstructorModule implements Module {
        private final ModuleRegistry moduleRegistry;

        public ModuleRegistryConstructorModule(ModuleRegistry moduleRegistry) {
            this.moduleRegistry = moduleRegistry;
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

    @ModuleInfo(id = "module-event-bus-constructor", name = "Module Event Bus Constructor")
    public static class ModuleEventBusConstructorModule implements Module {
        private final ModuleEventBus eventBus;

        public ModuleEventBusConstructorModule(ModuleEventBus eventBus) {
            this.eventBus = eventBus;
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

    @ModuleInfo(id = "module-instance-factory-constructor", name = "Module Instance Factory Constructor")
    public static class ModuleInstanceFactoryConstructorModule implements Module {
        private final ModuleInstanceFactory instanceFactory;

        public ModuleInstanceFactoryConstructorModule(ModuleInstanceFactory instanceFactory) {
            this.instanceFactory = instanceFactory;
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

    @ModuleInfo(id = "module-registry-field", name = "Module Registry Field")
    public static class ModuleRegistryFieldModule implements Module {
        @ModuleInject
        private ModuleRegistry moduleRegistry;

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

    @ModuleInfo(id = "private-event-bus-field", name = "Private Event Bus Field")
    public static class PrivateEventBusFieldModule implements Module {
        @ModuleInject
        private ModuleEventBus eventBus;

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

    @ModuleInfo(id = "constructor-and-field", name = "Constructor And Field")
    public static class ConstructorAndFieldModule implements Module {
        private final ModuleRegistry moduleRegistry;
        @ModuleInject
        private ModuleEventBus eventBus;

        public ConstructorAndFieldModule(ModuleRegistry moduleRegistry) {
            this.moduleRegistry = moduleRegistry;
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

    @ModuleInfo(id = "more-parameters-preferred", name = "More Parameters Preferred")
    public static class MoreParametersPreferredModule implements Module {
        private final ModuleRegistry moduleRegistry;

        public MoreParametersPreferredModule() {
            this.moduleRegistry = null;
        }

        public MoreParametersPreferredModule(ModuleRegistry moduleRegistry) {
            this.moduleRegistry = moduleRegistry;
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

    @ModuleInfo(id = "unresolvable-more-parameters", name = "Unresolvable More Parameters")
    public static class UnresolvableMoreParametersModule implements Module {
        private final ModuleRegistry moduleRegistry;
        private final String value;

        public UnresolvableMoreParametersModule(ModuleRegistry moduleRegistry) {
            this.moduleRegistry = moduleRegistry;
            this.value = null;
        }

        public UnresolvableMoreParametersModule(ModuleRegistry moduleRegistry, String value) {
            this.moduleRegistry = moduleRegistry;
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

    @ModuleInfo(id = "ambiguous-constructor", name = "Ambiguous Constructor")
    public static class AmbiguousConstructorModule implements Module {
        public AmbiguousConstructorModule(ModuleRegistry moduleRegistry) {
        }

        public AmbiguousConstructorModule(ModuleEventBus eventBus) {
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

    @ModuleInfo(id = "unmarked-field", name = "Unmarked Field")
    public static class UnmarkedFieldModule implements Module {
        private ModuleRegistry moduleRegistry;

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

    @ModuleInfo(id = "final-field", name = "Final Field")
    public static class FinalFieldModule implements Module {
        @ModuleInject
        private final ModuleRegistry moduleRegistry = null;

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

    @ModuleInfo(id = "static-field", name = "Static Field")
    public static class StaticFieldModule implements Module {
        @ModuleInject
        private static ModuleRegistry moduleRegistry;

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

    @ModuleInfo(id = "unsupported-field", name = "Unsupported Field")
    public static class UnsupportedFieldModule implements Module {
        @ModuleInject
        private String value;

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

    @ModuleInfo(id = "throwing-constructor", name = "Throwing Constructor")
    public static class ThrowingConstructorModule implements Module {
        public ThrowingConstructorModule() {
            throw new IllegalStateException("constructor failed");
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

    @ModuleInfo(id = "throwing-module-instantiation-constructor", name = "Throwing Module Instantiation Constructor")
    public static class ThrowingModuleInstantiationConstructorModule implements Module {
        public ThrowingModuleInstantiationConstructorModule() {
            throw new ModuleInstantiationException("inner", new IllegalStateException("inner cause"));
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
