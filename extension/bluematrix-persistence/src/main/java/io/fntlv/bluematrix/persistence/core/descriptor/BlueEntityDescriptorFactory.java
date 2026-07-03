package io.fntlv.bluematrix.persistence.core.descriptor;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.codec.Codec;
import br.com.finalcraft.everydatabase.query.IndexHint;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class BlueEntityDescriptorFactory {
    private final BlueEntityCodecProvider codecProvider;

    public BlueEntityDescriptorFactory(BlueEntityCodecProvider codecProvider) {
        if (codecProvider == null) {
            throw new IllegalArgumentException("codecProvider cannot be null");
        }
        this.codecProvider = codecProvider;
    }

    public <K, V> EntityDescriptor<K, V> create(Class<V> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType cannot be null");
        }
        BlueEntity entity = entityType.getAnnotation(BlueEntity.class);
        if (entity == null) {
            throw new BlueDescriptorException("Entity is missing @BlueEntity: " + entityType.getName());
        }
        if (entity.collection() == null || entity.collection().trim().isEmpty()) {
            throw new BlueDescriptorException("@BlueEntity.collection cannot be blank: " + entityType.getName());
        }

        KeyMember keyMember = findKeyMember(entityType);
        EntityDescriptor.Builder<K, V> builder = EntityDescriptor.builder(
                keyMember.keyType(),
                entityType
        );
        builder.collection(entity.collection().trim());
        builder.keyExtractor(keyMember.keyExtractor());
        builder.codec(createCodec(entityType, entity));
        applyIndexes(builder, entityType);
        applyOptimisticLock(builder, entityType, entity);
        if (entity.versioned()) {
            builder.versioned();
        }
        try {
            return builder.build();
        } catch (RuntimeException e) {
            throw new BlueDescriptorException("Failed to build descriptor for entity: " + entityType.getName(), e);
        }
    }

    private <V> Codec<V> createCodec(Class<V> entityType, BlueEntity entity) {
        Codec<V> codec;
        codec = codecProvider.create(entityType, entity);
        if (codec == null) {
            throw new BlueDescriptorException("Codec provider returned null for entity: " + entityType.getName());
        }
        return codec;
    }

    private <K, V> void applyIndexes(EntityDescriptor.Builder<K, V> builder, Class<V> entityType) {
        Class<?> current = entityType;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                BlueIndex index = field.getAnnotation(BlueIndex.class);
                if (index == null) {
                    continue;
                }
                builder.index(createIndexHint(field, index));
            }
            current = current.getSuperclass();
        }
    }

    private IndexHint createIndexHint(Field field, BlueIndex index) {
        String path = index.path().isEmpty() ? field.getName() : index.path();
        Class<?> javaType = index.type() == void.class ? field.getType() : index.type();
        IndexHint hint = createIndexHint(path, resolveIndexFieldType(javaType, field, path));
        if (index.order() == BlueIndexHint.Order.DESCENDING) {
            return hint.asDescending();
        }
        return hint;
    }

    private IndexHint createIndexHint(String path, BlueIndexHint.FieldType type) {
        switch (type) {
            case STRING:
                return IndexHint.string(path);
            case INT:
                return IndexHint.integer(path);
            case LONG:
                return IndexHint.bigInt(path);
            case DOUBLE:
                return IndexHint.decimal(path);
            case BOOLEAN:
                return IndexHint.bool(path);
            case TIMESTAMP:
                return IndexHint.timestamp(path);
            default:
                throw new BlueDescriptorException("Unknown BlueIndex field type: " + type);
        }
    }

    private BlueIndexHint.FieldType resolveIndexFieldType(Class<?> javaType, Field field, String path) {
        if (javaType == String.class) {
            return BlueIndexHint.FieldType.STRING;
        }
        if (javaType == int.class || javaType == Integer.class) {
            return BlueIndexHint.FieldType.INT;
        }
        if (javaType == long.class || javaType == Long.class) {
            return BlueIndexHint.FieldType.LONG;
        }
        if (javaType == float.class || javaType == Float.class
                || javaType == double.class || javaType == Double.class) {
            return BlueIndexHint.FieldType.DOUBLE;
        }
        if (javaType == boolean.class || javaType == Boolean.class) {
            return BlueIndexHint.FieldType.BOOLEAN;
        }
        if (javaType == Instant.class || javaType == LocalDateTime.class) {
            return BlueIndexHint.FieldType.TIMESTAMP;
        }
        throw new BlueDescriptorException("@BlueIndex on '" + location(field) + "' (path=\"" + path + "\"): "
                + "cannot auto-detect index type for Java type '" + javaType.getName() + "'");
    }

    private <K, V> void applyOptimisticLock(EntityDescriptor.Builder<K, V> builder, Class<V> entityType, BlueEntity entity) {
        Field lockField = findOptimisticLockField(entityType);
        if (lockField == null) {
            return;
        }
        if (entity.versioned()) {
            throw new BlueDescriptorException("@BlueEntity(versioned = true) cannot be combined with @BlueOptimisticLock: "
                    + entityType.getName());
        }
        lockField.setAccessible(true);
        builder.version(createVersionGetter(lockField), createVersionSetter(lockField));
    }

    private Field findOptimisticLockField(Class<?> entityType) {
        Field found = null;
        Class<?> current = entityType;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(BlueOptimisticLock.class)) {
                    continue;
                }
                if (found != null) {
                    throw new BlueDescriptorException("@BlueOptimisticLock: only one field per entity may carry the annotation, but "
                            + entityType.getName() + " has it on both '" + location(found)
                            + "' and '" + location(field) + "'");
                }
                validateOptimisticLockField(field);
                found = field;
            }
            current = current.getSuperclass();
        }
        return found;
    }

    private void validateOptimisticLockField(Field field) {
        if (field.getType() != long.class && field.getType() != Long.class) {
            throw new BlueDescriptorException("@BlueOptimisticLock on '" + location(field)
                    + "': the field must be of type long or Long");
        }
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            throw new BlueDescriptorException("@BlueOptimisticLock on '" + location(field)
                    + "': the field must not be static");
        }
        if (Modifier.isFinal(modifiers)) {
            throw new BlueDescriptorException("@BlueOptimisticLock on '" + location(field)
                    + "': the field must not be final");
        }
    }

    private <V> Function<V, Long> createVersionGetter(final Field field) {
        return new Function<V, Long>() {
            @Override
            public Long apply(V entity) {
                Object value = readAccessible(field, new AccessibleReader() {
                    @Override
                    public Object read() throws Exception {
                        return field.get(entity);
                    }
                });
                return value == null ? 0L : (Long) value;
            }
        };
    }

    private <V> BiConsumer<V, Long> createVersionSetter(final Field field) {
        return new BiConsumer<V, Long>() {
            @Override
            public void accept(final V entity, final Long version) {
                readAccessible(field, new AccessibleReader() {
                    @Override
                    public Object read() throws Exception {
                        field.set(entity, version);
                        return null;
                    }
                });
            }
        };
    }

    private KeyMember findKeyMember(Class<?> entityType) {
        KeyMember keyMember = null;
        Class<?> current = entityType;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(BlueKey.class)) {
                    continue;
                }
                validateKeyField(field, entityType);
                keyMember = requireSingleKey(keyMember, new FieldKeyMember(field), entityType);
            }
            for (Method method : current.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(BlueKey.class)) {
                    continue;
                }
                validateKeyMethod(method, entityType);
                keyMember = requireSingleKey(keyMember, new MethodKeyMember(method), entityType);
            }
            current = current.getSuperclass();
        }
        if (keyMember == null) {
            throw new BlueDescriptorException("Entity is missing @BlueKey field or method: " + entityType.getName());
        }
        return keyMember;
    }

    private KeyMember requireSingleKey(KeyMember existing, KeyMember next, Class<?> entityType) {
        if (existing != null) {
            throw new BlueDescriptorException("Entity declares multiple @BlueKey members: " + entityType.getName());
        }
        return next;
    }

    private void validateKeyField(Field field, Class<?> entityType) {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new BlueDescriptorException("@BlueKey field cannot be static: "
                    + entityType.getName() + "." + field.getName());
        }
    }

    private void validateKeyMethod(Method method, Class<?> entityType) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new BlueDescriptorException("@BlueKey method cannot be static: "
                    + entityType.getName() + "." + method.getName());
        }
        if (method.getParameterTypes().length != 0) {
            throw new BlueDescriptorException("@BlueKey method must not declare parameters: "
                    + entityType.getName() + "." + method.getName());
        }
        if (Void.TYPE.equals(method.getReturnType())) {
            throw new BlueDescriptorException("@BlueKey method must return a key value: "
                    + entityType.getName() + "." + method.getName());
        }
    }

    private static String location(Field field) {
        return field.getDeclaringClass().getSimpleName() + "." + field.getName();
    }

    private static Object readAccessible(AccessibleObject member, AccessibleReader reader) {
        boolean accessible = member.isAccessible();
        try {
            if (!accessible) {
                member.setAccessible(true);
            }
            return reader.read();
        } catch (Exception e) {
            throw new BlueDescriptorException("Failed to read @BlueKey member", e);
        } finally {
            if (!accessible) {
                member.setAccessible(false);
            }
        }
    }

    private interface AccessibleReader {
        Object read() throws Exception;
    }

    private interface KeyMember {
        <K> Class<K> keyType();

        <K, V> Function<V, K> keyExtractor();
    }

    private static final class FieldKeyMember implements KeyMember {
        private final Field field;

        private FieldKeyMember(Field field) {
            this.field = field;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K> Class<K> keyType() {
            return (Class<K>) field.getType();
        }

        @Override
        public <K, V> Function<V, K> keyExtractor() {
            return new Function<V, K>() {
                @Override
                @SuppressWarnings("unchecked")
                public K apply(final V entity) {
                    return (K) readAccessible(field, new AccessibleReader() {
                        @Override
                        public Object read() throws Exception {
                            return field.get(entity);
                        }
                    });
                }
            };
        }
    }

    private static final class MethodKeyMember implements KeyMember {
        private final Method method;

        private MethodKeyMember(Method method) {
            this.method = method;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K> Class<K> keyType() {
            return (Class<K>) method.getReturnType();
        }

        @Override
        public <K, V> Function<V, K> keyExtractor() {
            return new Function<V, K>() {
                @Override
                @SuppressWarnings("unchecked")
                public K apply(final V entity) {
                    return (K) readAccessible(method, new AccessibleReader() {
                        @Override
                        public Object read() throws Exception {
                            return method.invoke(entity);
                        }
                    });
                }
            };
        }
    }
}
