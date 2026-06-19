package io.fntlv.bluematrix.persistence.core;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.Storage;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntityDescriptorFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlueStorage {
    private final Map<Class<?>, EntityDescriptor<?, ?>> descriptors = new ConcurrentHashMap<Class<?>, EntityDescriptor<?, ?>>();
    private final BlueEntityDescriptorFactory descriptorFactory = new BlueEntityDescriptorFactory();
    private volatile Storage storage;

    public BlueStorage() {
        this(null);
    }

    public BlueStorage(Storage storage) {
        if (storage != null) {
            initialize(storage);
        }
    }

    public boolean available() {
        return storage != null;
    }

    public Storage storage() {
        Storage current = storage;
        if (current == null) {
            throw new IllegalStateException("BlueStorage is not initialized");
        }
        return current;
    }

    public Storage getStorage() {
        return storage();
    }

    public <K, V> Repository<K, V> repository(EntityDescriptor<K, V> descriptor) {
        return storage().repository(descriptor);
    }

    @SuppressWarnings("unchecked")
    public <K, V> EntityDescriptor<K, V> descriptor(Class<V> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType cannot be null");
        }
        EntityDescriptor<?, ?> descriptor = descriptors.get(entityType);
        if (descriptor != null) {
            return (EntityDescriptor<K, V>) descriptor;
        }
        synchronized (descriptors) {
            descriptor = descriptors.get(entityType);
            if (descriptor == null) {
                descriptor = descriptorFactory.create(entityType);
                descriptors.put(entityType, descriptor);
            }
            return (EntityDescriptor<K, V>) descriptor;
        }
    }

    public <K, V> Repository<K, V> repository(Class<V> entityType) {
        return storage().repository(descriptor(entityType));
    }

    public synchronized void initialize(Storage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        if (this.storage != null) {
            throw new IllegalStateException("BlueStorage is already initialized");
        }
        storage.init().join();
        this.storage = storage;
    }

    public void close() {
        Storage current = storage;
        if (current != null) {
            current.close().join();
        }
    }
}
