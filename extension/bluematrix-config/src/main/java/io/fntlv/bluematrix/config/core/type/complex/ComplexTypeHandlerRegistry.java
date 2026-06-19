package io.fntlv.bluematrix.config.core.type.complex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ComplexTypeHandlerRegistry {

    private final List<ComplexTypeHandler<?>> handlers = new ArrayList<>();

    public ComplexTypeHandlerRegistry() {
        registerDefaults();
    }

    public ComplexTypeHandlerRegistry register(ComplexTypeHandler<?> handler) {
        handlers.add(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    public Optional<ComplexTypeHandler<?>> find(Class<?> type) {
        for (ComplexTypeHandler<?> handler : handlers) {
            if (handler.supports(type)) {
                return Optional.of(handler);
            }
        }
        return Optional.empty();
    }

    public void clear() {
        clearAll();
        registerDefaults();
    }

    public void clearAll() {
        handlers.clear();
    }

    public List<ComplexTypeHandler<?>> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }

    private void registerDefaults() {
        register(uuid());
        register(set());
        register(linkedHashMapValues());
    }

    private ComplexTypeHandler<UUID> uuid() {
        return ComplexTypeHandlers.forType(UUID.class)
                .onConfigSave((section, value) -> section.set("", value.toString()))
                .onConfigLoad((section, type) -> UUID.fromString(section.getString("")))
                .onStringSerialize(UUID::toString)
                .onStringDeserialize((value, type) -> UUID.fromString(value));
    }

    private ComplexTypeHandler<Set> set() {
        return ComplexTypeHandlers.forType(Set.class)
                .onConfigSave((section, value) -> section.set("", new ArrayList<>(value)));
    }

    @SuppressWarnings("unchecked")
    private ComplexTypeHandler<Collection> linkedHashMapValues() {
        Class<Collection> valuesType = (Class<Collection>) new LinkedHashMap<>().values().getClass();
        return ComplexTypeHandlers.forType(valuesType)
                .onConfigSave((section, value) -> section.set("", new ArrayList<>(value)));
    }
}
