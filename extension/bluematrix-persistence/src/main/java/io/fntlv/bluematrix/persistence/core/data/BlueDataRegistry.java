package io.fntlv.bluematrix.persistence.core.data;

import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BlueDataRegistry {
    private final Map<Class<?>, BlueDataDefinition<?, ?>> definitionsByType =
            new ConcurrentHashMap<>();
    private final Map<String, BlueDataDefinition<?, ?>> definitionsByCollection =
            new ConcurrentHashMap<>();

    public void register(BlueDataDefinition<?, ?> definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        BlueDataDefinition<?, ?> existingType = definitionsByType.putIfAbsent(definition.type(), definition);
        if (existingType != null && existingType != definition) {
            throw new IllegalArgumentException("Duplicate BlueData type: " + definition.type().getName());
        }
        BlueDataDefinition<?, ?> existingCollection =
                definitionsByCollection.putIfAbsent(definition.collection(), definition);
        if (existingCollection != null && existingCollection != definition) {
            throw new IllegalArgumentException("Duplicate BlueData collection: " + definition.collection());
        }
    }

    public Optional<BlueDataDefinition<?, ?>> find(Class<?> dataType) {
        if (dataType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitionsByType.get(dataType));
    }

    public Optional<BlueDataDefinition<?, ?>> find(String collection) {
        if (collection == null || collection.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitionsByCollection.get(collection.trim()));
    }

    public Collection<BlueDataDefinition<?, ?>> definitions() {
        return Collections.unmodifiableCollection(definitionsByType.values());
    }

    public void clear() {
        definitionsByType.clear();
        definitionsByCollection.clear();
    }
}
