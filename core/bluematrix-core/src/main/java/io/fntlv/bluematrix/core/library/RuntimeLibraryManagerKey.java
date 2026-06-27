package io.fntlv.bluematrix.core.library;

import java.util.Objects;

final class RuntimeLibraryManagerKey {
    private final BlueMatrixLibraryScope scope;
    private final String qualifier;

    RuntimeLibraryManagerKey(BlueMatrixLibraryScope scope, String qualifier) {
        this.scope = scope;
        this.qualifier = RuntimeLibraryNames.normalize(qualifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RuntimeLibraryManagerKey)) {
            return false;
        }
        RuntimeLibraryManagerKey that = (RuntimeLibraryManagerKey) o;
        return scope == that.scope && Objects.equals(qualifier, that.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, qualifier);
    }
}
