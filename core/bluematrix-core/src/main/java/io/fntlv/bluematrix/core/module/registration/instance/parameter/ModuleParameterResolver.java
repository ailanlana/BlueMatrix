package io.fntlv.bluematrix.core.module.registration.instance.parameter;

import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

public interface ModuleParameterResolver {

    boolean supports(Class<?> parameterType);

    Object resolve(Class<?> parameterType, ModuleCandidate candidate);
}
