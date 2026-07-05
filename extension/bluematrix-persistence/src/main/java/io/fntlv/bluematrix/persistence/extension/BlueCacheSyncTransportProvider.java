package io.fntlv.bluematrix.persistence.extension;

import br.com.finalcraft.everydatabase.manager.sync.CacheSyncTransport;

public interface BlueCacheSyncTransportProvider {
    CacheSyncTransport getCacheSyncTransport();
}
