package io.fntlv.bluematrix.core.module.registration.resolver;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import lombok.Getter;

import java.util.*;

public class DependencyGraph {
    private final Map<String, Node> nodes = new HashMap<>();

    public static class Node {
        @Getter
        private final ModuleCandidate module;
        private final Set<Node> dependencies = new HashSet<>();
        private final Set<Node> dependents = new HashSet<>();

        public Node(ModuleCandidate module) {
            this.module = module;
        }

        public Set<Node> getDependencies() {
            return Collections.unmodifiableSet(dependencies);
        }

        public Set<Node> getDependents() {
            return Collections.unmodifiableSet(dependents);
        }
    }

    private void addModule(ModuleCandidate module) {
        nodes.putIfAbsent(module.id(), new Node(module));
    }

    private void addDependency(String sourceId, String targetId) {
        Node source = nodes.get(sourceId);
        Node target = nodes.get(targetId);
        if (source != null && target != null) {
            source.dependencies.add(target);
            target.dependents.add(source);
        }
    }

    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public static DependencyGraph build(List<ModuleCandidate> modules) {
        DependencyGraph graph = new DependencyGraph();
        modules.forEach(graph::addModule);
        modules.forEach(module -> {
            String sourceId = module.id();
            Arrays.stream(module.dependencies())
                    .forEach(targetId -> graph.addDependency(sourceId, targetId));
            Arrays.stream(module.softDependencies())
                    .forEach(targetId -> graph.addDependency(sourceId, targetId));
        });
        return graph;
    }
}
