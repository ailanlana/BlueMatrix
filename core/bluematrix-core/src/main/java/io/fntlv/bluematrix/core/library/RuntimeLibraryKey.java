package io.fntlv.bluematrix.core.library;

import java.util.Objects;

final class RuntimeLibraryKey {
    private final BlueMatrixLibraryScope scope;
    private final String qualifier;
    private final String coordinate;

    RuntimeLibraryKey(BlueMatrixLibraryScope scope, String qualifier, String coordinate) {
        this.scope = scope;
        this.qualifier = RuntimeLibraryNames.normalize(qualifier);
        this.coordinate = coordinate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RuntimeLibraryKey)) {
            return false;
        }
        RuntimeLibraryKey that = (RuntimeLibraryKey) o;
        return scope == that.scope
                && Objects.equals(qualifier, that.qualifier)
                && Objects.equals(coordinate, that.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, qualifier, coordinate);
    }
}
