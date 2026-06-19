package io.fntlv.bluematrix.config.core.type.complex;

public final class ComplexTypeHandlers {

    private ComplexTypeHandlers() {
    }

    public static <T> ComplexTypeHandler<T> forType(Class<T> type) {
        return new ComplexTypeHandler<>(type);
    }
}
