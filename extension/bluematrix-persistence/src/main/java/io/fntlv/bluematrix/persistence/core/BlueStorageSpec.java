package io.fntlv.bluematrix.persistence.core;

import br.com.finalcraft.everydatabase.Storage;
import br.com.finalcraft.everydatabase.StorageConfig;
import br.com.finalcraft.everydatabase.Storages;
import br.com.finalcraft.everydatabase.modules.localfile.LocalFileConfig;
import br.com.finalcraft.everydatabase.modules.memory.InMemoryConfig;
import br.com.finalcraft.everydatabase.modules.mongo.MongoConfig;
import br.com.finalcraft.everydatabase.modules.sql.SqlConfig;

public final class BlueStorageSpec {
    private final StorageConfig config;
    private final StorageFactory storageFactory;

    private BlueStorageSpec(StorageConfig config, StorageFactory storageFactory) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (storageFactory == null) {
            throw new IllegalArgumentException("storageFactory cannot be null");
        }
        this.config = config;
        this.storageFactory = storageFactory;
    }

    public static BlueStorageSpec sql(final SqlConfig config) {
        return new BlueStorageSpec(config, new StorageFactory() {
            @Override
            public Storage create() {
                return Storages.createSQL(config);
            }
        });
    }

    public static BlueStorageSpec postgresql(final SqlConfig config) {
        return new BlueStorageSpec(config, new StorageFactory() {
            @Override
            public Storage create() {
                return Storages.createPostgreSQL(config);
            }
        });
    }

    public static BlueStorageSpec h2(final SqlConfig config) {
        return new BlueStorageSpec(config, new StorageFactory() {
            @Override
            public Storage create() {
                return Storages.createH2(config);
            }
        });
    }

    public static BlueStorageSpec mongo(final MongoConfig config) {
        return new BlueStorageSpec(config, new StorageFactory() {
            @Override
            public Storage create() {
                return Storages.createMongo(config);
            }
        });
    }

    public static BlueStorageSpec localFile(final LocalFileConfig config) {
        return new BlueStorageSpec(config, new StorageFactory() {
            @Override
            public Storage create() {
                return Storages.createLocalFile(config);
            }
        });
    }

    public static BlueStorageSpec inMemory() {
        return new BlueStorageSpec(new InMemoryConfig(), new StorageFactory() {
            @Override
            public Storage create() {
                return Storages.createInMemory();
            }
        });
    }

    public StorageConfig config() {
        return config;
    }

    public Storage createStorage() {
        return storageFactory.create();
    }

    private interface StorageFactory {
        Storage create();
    }
}
