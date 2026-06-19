package io.fntlv.bluematrix.core.module.registration.resolver;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologyDependencyResolverTest {

    private final TopologyDependencyResolver resolver = new TopologyDependencyResolver();

    @Test
    void dependenciesWinOverLoadOrder() {
        List<ModuleCandidate> ordered = resolver.resolve(Arrays.asList(
                provided(HighDependsOnLow.class),
                provided(LowDependency.class)
        ));

        assertEquals(Arrays.asList("low-dependency", "high-dependent"), ids(ordered));
    }

    @Test
    void loadOrderSortsOnlyReadyCandidates() {
        List<ModuleCandidate> ordered = resolver.resolve(Arrays.asList(
                provided(NormalBeta.class),
                provided(HighestAlpha.class),
                provided(LowGamma.class)
        ));

        assertEquals(Arrays.asList("alpha", "beta", "gamma"), ids(ordered));
    }

    @Test
    void idSortsSamePriorityCandidates() {
        List<ModuleCandidate> ordered = resolver.resolve(Arrays.asList(
                provided(NormalBeta.class),
                provided(NormalAlpha.class)
        ));

        assertEquals(Arrays.asList("alpha-normal", "beta"), ids(ordered));
    }

    @Test
    void presentSoftDependencyAffectsOrder() {
        List<ModuleCandidate> ordered = resolver.resolve(Arrays.asList(
                provided(SoftDependsOnBeta.class),
                provided(NormalBeta.class)
        ));

        assertEquals(Arrays.asList("beta", "soft-dependent"), ids(ordered));
    }

    @Test
    void missingSoftDependencyDoesNotBlockOrder() {
        List<ModuleCandidate> ordered = resolver.resolve(Arrays.asList(
                provided(SoftDependsOnMissing.class),
                provided(NormalBeta.class)
        ));

        assertEquals(Arrays.asList("missing-soft-dependent", "beta"), ids(ordered));
    }

    @Test
    void circularDependenciesAreMarked() {
        List<ModuleCandidate> ordered = resolver.resolve(Arrays.asList(
                provided(CircularA.class),
                provided(CircularB.class)
        ));

        assertTrue(ordered.isEmpty());
    }

    @Test
    void missingRequiredDependencyReturnsFailure() {
        List<ModuleCandidate> ordered = resolver.resolve(Arrays.asList(
                provided(DependsOnMissing.class),
                provided(NormalBeta.class)
        ));

        assertEquals(Arrays.asList("beta"), ids(ordered));
    }

    private static ModuleCandidate provided(Class<? extends Module> moduleClass) {
        return new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    private static List<String> ids(List<ModuleCandidate> modules) {
        return modules.stream()
                .map(module -> module.getModuleInfo().id())
                .collect(Collectors.toList());
    }

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

    @ModuleInfo(id = "high-dependent", name = "High dependent", dependencies = "low-dependency", loadOrder = ModuleInfo.LoadOrder.HIGHEST)
    private static class HighDependsOnLow extends TestModule {
    }

    @ModuleInfo(id = "low-dependency", name = "Low dependency", loadOrder = ModuleInfo.LoadOrder.LOWEST)
    private static class LowDependency extends TestModule {
    }

    @ModuleInfo(id = "alpha", name = "Alpha", loadOrder = ModuleInfo.LoadOrder.HIGHEST)
    private static class HighestAlpha extends TestModule {
    }

    @ModuleInfo(id = "beta", name = "Beta")
    private static class NormalBeta extends TestModule {
    }

    @ModuleInfo(id = "gamma", name = "Gamma", loadOrder = ModuleInfo.LoadOrder.LOW)
    private static class LowGamma extends TestModule {
    }

    @ModuleInfo(id = "alpha-normal", name = "Alpha normal")
    private static class NormalAlpha extends TestModule {
    }

    @ModuleInfo(id = "soft-dependent", name = "Soft dependent", softDependencies = "beta", loadOrder = ModuleInfo.LoadOrder.HIGHEST)
    private static class SoftDependsOnBeta extends TestModule {
    }

    @ModuleInfo(id = "missing-soft-dependent", name = "Missing soft dependent", softDependencies = "missing", loadOrder = ModuleInfo.LoadOrder.HIGHEST)
    private static class SoftDependsOnMissing extends TestModule {
    }

    @ModuleInfo(id = "missing-required-dependent", name = "Missing required dependent", dependencies = "missing", loadOrder = ModuleInfo.LoadOrder.HIGHEST)
    private static class DependsOnMissing extends TestModule {
    }

    @ModuleInfo(id = "circular-a", name = "Circular A", dependencies = "circular-b")
    private static class CircularA extends TestModule {
    }

    @ModuleInfo(id = "circular-b", name = "Circular B", dependencies = "circular-a")
    private static class CircularB extends TestModule {
    }
}
