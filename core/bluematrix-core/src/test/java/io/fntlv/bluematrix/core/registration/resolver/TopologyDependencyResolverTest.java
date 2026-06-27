package io.fntlv.bluematrix.core.module.registration.resolver;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationStageResult;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssueType;
import io.fntlv.bluematrix.core.module.registration.issue.issues.CircularDependencyIssue;
import io.fntlv.bluematrix.core.module.registration.issue.issues.MissingRequiredDependencyIssue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
    void circularDependenciesAreReturnedAsIssues() {
        ModuleRegistrationStageResult<ModuleCandidate> result = resolver.resolveWithResult(Arrays.asList(
                provided(CircularA.class),
                provided(CircularB.class)
        ));

        assertTrue(result.passed().isEmpty());
        assertEquals(2, result.issues().size());
        CircularDependencyIssue issue = assertInstanceOf(CircularDependencyIssue.class, result.issues().all().get(0));
        assertIssue(issue, ModuleRegistrationIssueType.CIRCULAR_DEPENDENCY);
        assertTrue(idsFromIssues(result.issues().all()).contains("circular-a"));
        assertTrue(idsFromIssues(result.issues().all()).contains("circular-b"));
        assertTrue(issue.cycleModuleIds().contains("circular-a"));
        assertTrue(issue.cycleModuleIds().contains("circular-b"));
    }

    @Test
    void missingRequiredDependencyReturnsFailure() {
        List<ModuleCandidate> ordered = resolver.resolve(Arrays.asList(
                provided(DependsOnMissing.class),
                provided(NormalBeta.class)
        ));

        assertEquals(Arrays.asList("beta"), ids(ordered));
    }

    @Test
    void missingRequiredDependencyIsReturnedAsIssue() {
        ModuleRegistrationStageResult<ModuleCandidate> result = resolver.resolveWithResult(Arrays.asList(
                provided(DependsOnMissing.class),
                provided(NormalBeta.class)
        ));

        assertEquals(Arrays.asList("beta"), ids(result.passed()));
        assertEquals(1, result.issues().size());
        MissingRequiredDependencyIssue issue = assertInstanceOf(MissingRequiredDependencyIssue.class, result.issues().all().get(0));
        assertIssue(issue, ModuleRegistrationIssueType.MISSING_REQUIRED_DEPENDENCY);
        assertEquals("missing-required-dependent", issue.moduleId());
        assertEquals(Arrays.asList("missing"), issue.missingDependencyIds());
    }

    private static ModuleCandidate provided(Class<? extends Module> moduleClass) {
        return new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    private static List<String> ids(List<ModuleCandidate> modules) {
        return modules.stream()
                .map(module -> module.id())
                .collect(Collectors.toList());
    }

    private static List<String> idsFromIssues(List<ModuleRegistrationIssue> issues) {
        return issues.stream()
                .map(ModuleRegistrationIssue::moduleId)
                .collect(Collectors.toList());
    }

    private static void assertIssue(ModuleRegistrationIssue issue, ModuleRegistrationIssueType type) {
        assertEquals(type, issue.type());
        assertTrue(issue.message().length() > 0);
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
