package io.fntlv.bluematrix.core.module.registration.resolver;

import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.resolver.DependencyGraph.Node;

import java.util.*;
import java.util.stream.Collectors;

public class TopologyDependencyResolver implements DependencyResolver {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(TopologyDependencyResolver.class);

    @Override
    public List<ModuleCandidate> resolve(List<ModuleCandidate> modules) {
        List<ModuleCandidate> dependencyReadyModules = removeMissingDependencies(modules);
        DependencyGraph graph = DependencyGraph.build(dependencyReadyModules);
        SortResult sortResult = kahnTopologicalSort(graph);

        for (ModuleCandidate module : sortResult.circularModules) {
            logSkip(module, "Circular dependency detected with modules: " + String.join(", ", ids(sortResult.circularModules)));
        }

        return sortResult.loadOrder;
    }

    private List<ModuleCandidate> removeMissingDependencies(List<ModuleCandidate> modules) {
        Map<String, ModuleCandidate> remainingById = modules.stream()
                .collect(Collectors.toMap(module -> module.getModuleInfo().id(), module -> module, (first, second) -> first, LinkedHashMap::new));
        Map<String, List<String>> missingDependsById = new LinkedHashMap<>();

        boolean changed;
        do {
            changed = false;
            for (ModuleCandidate module : new ArrayList<>(remainingById.values())) {
                List<String> missingDepends = getMissingRequiredDepends(module, remainingById.keySet());
                if (!missingDepends.isEmpty()) {
                    remainingById.remove(module.getModuleInfo().id());
                    missingDependsById.put(module.getModuleInfo().id(), missingDepends);
                    changed = true;
                }
            }
        } while (changed);

        for (ModuleCandidate module : modules) {
            List<String> missingDepends = missingDependsById.get(module.getModuleInfo().id());
            if (missingDepends != null) {
                logSkip(module, "Missing required dependencies: " + String.join(", ", missingDepends));
            }
        }
        return new ArrayList<>(remainingById.values());
    }

    private List<String> getMissingRequiredDepends(ModuleCandidate module, Set<String> availableModuleIds) {
        return Arrays.stream(module.getModuleInfo().dependencies())
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
        ModuleInfo firstInfo = first.getModule().getModuleInfo();
        ModuleInfo secondInfo = second.getModule().getModuleInfo();

        int loadOrderCompare = Integer.compare(firstInfo.loadOrder().ordinal(), secondInfo.loadOrder().ordinal());
        if (loadOrderCompare != 0) {
            return loadOrderCompare;
        }

        return firstInfo.id().compareTo(secondInfo.id());
    }

    private List<ModuleCandidate> toModules(List<Node> nodes) {
        return nodes.stream()
                .map(Node::getModule)
                .collect(Collectors.toList());
    }

    private List<String> ids(List<ModuleCandidate> modules) {
        return modules.stream()
                .map(module -> module.getModuleInfo().id())
                .collect(Collectors.toList());
    }

    private void logSkip(ModuleCandidate module, String reason) {
        ModuleInfo info = module.getModuleInfo();
        LOGGER.warn(
                "Skipping module: {} ({}) - {}",
                info.name(),
                info.id(),
                reason
        );
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
