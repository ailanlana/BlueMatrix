package io.fntlv.bluematrix.config.core.type.complex.function;

@FunctionalInterface
public interface StringDeserializeFunction<T> {

    T deserialize(String value, Class<T> type);
}
