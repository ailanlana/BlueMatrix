package io.fntlv.bluematrix.persistence.core.storage;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.Storage;
import br.com.finalcraft.everydatabase.manager.RefRegistry;
import io.fntlv.bluematrix.persistence.core.data.BlueDataRegistry;

public class BlueStorage {
    private final BlueDataRegistry registry = new BlueDataRegistry();
    private final RefRegistry refRegistry = new RefRegistry();
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

    public BlueDataRegistry registry() {
        return registry;
    }

    public RefRegistry refRegistry() {
        return refRegistry;
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
