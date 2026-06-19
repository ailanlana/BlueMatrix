package io.fntlv.bluematrix.config.core.type.complex.function;

@FunctionalInterface
public interface StringSerializeFunction<T> {

    String serialize(T value);
}