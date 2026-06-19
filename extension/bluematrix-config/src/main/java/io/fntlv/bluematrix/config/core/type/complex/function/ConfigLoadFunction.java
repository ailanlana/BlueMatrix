package io.fntlv.bluematrix.config.core.type.complex.function;

import io.fntlv.bluematrix.config.core.section.ConfigSection;

@FunctionalInterface
public interface ConfigLoadFunction<T> {

    T load(ConfigSection section, Class<T> type);
}
