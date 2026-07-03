package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.persistence.core.data.BlueDataAccess;
import io.fntlv.bluematrix.persistence.core.data.BlueDataQueryAccess;

public interface ModulePersistenceContext {
    String moduleId();

    BlueDataAccess dataAccess();

    BlueDataQueryAccess queryAccess();
}
