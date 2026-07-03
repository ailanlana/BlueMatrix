package io.fntlv.bluematrix.persistence.core.storage.source;

import br.com.finalcraft.everydatabase.modules.mongo.MongoConfig;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorageSpec;

import java.time.Duration;
import java.util.Optional;

public interface BlueMongoStorageSource extends BlueStorageSource {
    String getConnectionString();

    String getDatabase();

    default long getConnectTimeoutMillis() {
        return -1L;
    }

    @Override
    default BlueStorageSpec toSpec(BlueStorageSourceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        Optional<Duration> connectTimeout = getConnectTimeoutMillis() > 0
                ? Optional.of(Duration.ofMillis(getConnectTimeoutMillis()))
                : Optional.<Duration>empty();
        return BlueStorageSpec.mongo(new MongoConfig(getConnectionString(), getDatabase(), connectTimeout));
    }
}
