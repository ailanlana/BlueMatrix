package io.fntlv.bluematrix.core.module.instance.parameter;

import io.fntlv.bluematrix.core.module.instance.InjectContext;

public interface ModuleParameterResolver {

    boolean supports(Class<?> parameterType, InjectContext context);

    Object resolve(Class<?> parameterType, InjectContext context);
}
