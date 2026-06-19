package io.fntlv.bluematrix.core;

import io.fntlv.bluematrix.core.event.ModuleEvent;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import lombok.Getter;

@Getter
public abstract class BlueMatrixContainerEvent implements ModuleEvent {

    private BlueMatrixContainerEvent() {
    }

    @Getter
    public static final class Created extends BlueMatrixContainerEvent {
        private final ModuleParameterResolverRegistry parameterResolvers;
        private final ModuleInstanceFactory instanceFactory;

        public Created(ModuleParameterResolverRegistry parameterResolvers, ModuleInstanceFactory instanceFactory) {
            if (parameterResolvers == null) {
                throw new IllegalArgumentException("parameterResolvers cannot be null");
            }
            if (instanceFactory == null) {
                throw new IllegalArgumentException("instanceFactory cannot be null");
            }
            this.parameterResolvers = parameterResolvers;
            this.instanceFactory = instanceFactory;
        }
    }
}
