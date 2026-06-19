package io.fntlv.bluematrix.persistence.core.descriptor;

public final class BlueIndexHint {
    private BlueIndexHint() {
    }

    public enum FieldType {
        STRING,
        INT,
        LONG,
        DOUBLE,
        BOOLEAN,
        TIMESTAMP
    }

    public enum Order {
        ASCENDING,
        DESCENDING
    }
}
