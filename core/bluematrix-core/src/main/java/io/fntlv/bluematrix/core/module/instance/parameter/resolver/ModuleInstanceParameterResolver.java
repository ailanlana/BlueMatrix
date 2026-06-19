package io.fntlv.bluematrix.core.module.instance.parameter.resolver;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.OtherInjectionContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;

public class ModuleInstanceParameterResolver implements ModuleParameterResolver {

    @Override
    public boolean supports(Class<?> parameterType, InjectContext context) {
        if (!(context instanceof OtherInjectionContext)) {
            return false;
        }
        Module moduleInstance = ((OtherInjectionContext) context).getModuleInstance();
        return Module.class.isAssignableFrom(parameterType)
                && parameterType.isAssignableFrom(moduleInstance.getClass());
    }

    @Override
    public Object resolve(Class<?> parameterType, InjectContext context) {
        return ((OtherInjectionContext) context).getModuleInstance();
    }
}
