package io.fntlv.bluematrix.core;

import io.fntlv.bluematrix.core.event.ModuleEvent;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolverRegistry;
import lombok.Getter;

@Getter
public abstract class BlueMatrixContainerEvent implements ModuleEvent {

    private BlueMatrixContainerEvent() {
    }

    @Getter
    public static final class Created extends BlueMatrixContainerEvent {
        private final ModuleParameterResolverRegistry parameterResolvers;

        public Created(ModuleParameterResolverRegistry parameterResolvers) {
            if (parameterResolvers == null) {
                throw new IllegalArgumentException("parameterResolvers cannot be null");
            }
            this.parameterResolvers = parameterResolvers;
        }
    }
}
