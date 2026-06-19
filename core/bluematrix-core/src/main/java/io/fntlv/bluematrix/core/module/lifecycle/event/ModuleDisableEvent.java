package io.fntlv.bluematrix.core.module.lifecycle.event;

import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.event.ModuleEvent;
import lombok.Getter;

@Getter
public abstract class ModuleDisableEvent implements ModuleEvent {
    private final ModuleContext context;

    private ModuleDisableEvent(ModuleContext context) {
        this.context = context;
    }

    public static final class Pre extends ModuleDisableEvent {
        public Pre(ModuleContext context) {
            super(context);
        }
    }

    public static final class Post extends ModuleDisableEvent {
        public Post(ModuleContext context) {
            super(context);
        }
    }

    @Getter
    public static final class Failed extends ModuleDisableEvent {
        private final Throwable cause;

        public Failed(ModuleContext context, Throwable cause) {
            super(context);
            this.cause = cause;
        }
    }
}
