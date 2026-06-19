package io.fntlv.bluematrix.config.core.type.complex.function;

import io.fntlv.bluematrix.config.core.section.ConfigSection;

@FunctionalInterface
public interface ConfigSaveFunction<T> {

    void save(ConfigSection section, T value);
}
