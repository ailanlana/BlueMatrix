package io.fntlv.bluematrix.core.module.registration.resolver;

import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationStageResult;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues;
import io.fntlv.bluematrix.core.module.registration.outcome.RegistrationOutcomeClassifier;
import io.fntlv.bluematrix.core.module.registration.resolver.DependencyGraph.Node;

import java.util.*;
import java.util.stream.Collectors;

public class TopologyDependencyResolver implements DependencyResolver {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(TopologyDependencyResolver.class);
    private final RegistrationOutcomeClassifier outcomes = new RegistrationOutcomeClassifier(LOGGER);

    @Override
    public ModuleRegistrationStageResult<ModuleCandidate> resolve(List<ModuleCandidate> modules) {
        List<ModuleRegistrationIssue> issues = new ArrayList<>();
        List<ModuleCandidate> dependencyReadyModules = removeMissingDependencies(modules, issues);
        DependencyGraph graph = DependencyGraph.build(dependencyReadyModules);
        SortResult sortResult = kahnTopologicalSort(graph);

        for (ModuleCandidate module : sortResult.circularModules) {
            List<String> circularIds = ids(sortResult.circularModules);
            issues.add(outcomes.circularDependency(module, circularIds));
        }

        return ModuleRegistrationStageResult.of(sortResult.loadOrder, new ModuleRegistrationIssues(issues));
    }

    private List<ModuleCandidate> removeMissingDependencies(List<ModuleCandidate> modules,
                                                            List<ModuleRegistrationIssue> issues) {
        Map<String, ModuleCandidate> remainingById = modules.stream()
                .collect(Collectors.toMap(module -> module.id(), module -> module, (first, second) -> first, LinkedHashMap::new));
        Map<String, List<String>> missingDependsById = new LinkedHashMap<>();

        boolean changed;
        do {
            changed = false;
            for (ModuleCandidate module : new ArrayList<>(remainingById.values())) {
                List<String> missingDepends = getMissingRequiredDepends(module, remainingById.keySet());
                if (!missingDepends.isEmpty()) {
                    remainingById.remove(module.id());
                    missingDependsById.put(module.id(), missingDepends);
                    changed = true;
                }
            }
        } while (changed);

        for (ModuleCandidate module : modules) {
            List<String> missingDepends = missingDependsById.get(module.id());
            if (missingDepends != null) {
                issues.add(outcomes.missingRequiredDependency(module, missingDepends));
            }
        }
        return new ArrayList<>(remainingById.values());
    }

    private List<String> getMissingRequiredDepends(ModuleCandidate module, Set<String> availableModuleIds) {
        return Arrays.stream(module.dependencies())
                .filter(dependencyId -> !availableModuleIds.contains(dependencyId))
                .collect(Collectors.toList());
    }

    private SortResult kahnTopologicalSort(DependencyGraph graph) {
        Map<Node, Integer> outDegree = new HashMap<>();
        Queue<Node> queue = new PriorityQueue<>(this::compareNodePriority);
        List<Node> result = new ArrayList<>();

        graph.getAllNodes().forEach(node -> {
            int degree = node.getDependencies().size();
            outDegree.put(node, degree);
            if (degree == 0) {
                queue.offer(node);
            }
        });

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            result.add(node);

            for (DependencyGraph.Node dep : node.getDependents()) {
                int newDegree = outDegree.get(dep) - 1;
                outDegree.put(dep, newDegree);
                if (newDegree == 0) {
                    queue.offer(dep);
                }
            }
        }

        if (result.size() != graph.getAllNodes().size()) {
            List<ModuleCandidate> circularModules = outDegree.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .map(Node::getModule)
                    .collect(Collectors.toList());
            return new SortResult(toModules(result), circularModules);
        }
        return new SortResult(toModules(result), Collections.emptyList());
    }

    private int compareNodePriority(Node first, Node second) {
        ModuleCandidate firstModule = first.getModule();
        ModuleCandidate secondModule = second.getModule();

        int loadOrderCompare = Integer.compare(firstModule.loadOrder().ordinal(), secondModule.loadOrder().ordinal());
        if (loadOrderCompare != 0) {
            return loadOrderCompare;
        }

        return firstModule.id().compareTo(secondModule.id());
    }

    private List<ModuleCandidate> toModules(List<Node> nodes) {
        return nodes.stream()
                .map(Node::getModule)
                .collect(Collectors.toList());
    }

    private List<String> ids(List<ModuleCandidate> modules) {
        return modules.stream()
                .map(module -> module.id())
                .collect(Collectors.toList());
    }

    private static class SortResult {
        private final List<ModuleCandidate> loadOrder;
        private final List<ModuleCandidate> circularModules;

        private SortResult(List<ModuleCandidate> loadOrder, List<ModuleCandidate> circularModules) {
            this.loadOrder = loadOrder;
            this.circularModules = circularModules;
        }
    }

    //TODO Tarjan
}
