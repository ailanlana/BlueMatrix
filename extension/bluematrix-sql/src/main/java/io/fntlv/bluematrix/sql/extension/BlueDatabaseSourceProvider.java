package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.sql.core.BlueDatabaseSource;

public interface BlueDatabaseSourceProvider {
    BlueDatabaseSource getDatabaseSource();
}
