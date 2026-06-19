package io.fntlv.bluematrix.core.module.lifecycle.event;

import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.event.ModuleEvent;
import lombok.Getter;

@Getter
public abstract class ModuleLoadEvent implements ModuleEvent {
    private final ModuleContext context;

    private ModuleLoadEvent(ModuleContext context) {
        this.context = context;
    }

    @Getter
    public static final class Pre extends ModuleLoadEvent {
        private String errorSource;
        private String errorMessage;
        private Throwable errorCause;

        public Pre(ModuleContext context) {
            super(context);
        }

        public void error(String source, String message, Throwable cause) {
            if (source == null || source.trim().isEmpty()) {
                throw new IllegalArgumentException("source cannot be blank");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("message cannot be blank");
            }
            if (cause == null) {
                throw new IllegalArgumentException("cause cannot be null");
            }
            this.errorSource = source;
            this.errorMessage = message;
            this.errorCause = cause;
        }

        public boolean hasError() {
            return errorCause != null;
        }
    }

    public static final class Post extends ModuleLoadEvent {
        public Post(ModuleContext context) {
            super(context);
        }
    }

    @Getter
    public static final class Failed extends ModuleLoadEvent {
        private final Throwable cause;

        public Failed(ModuleContext context, Throwable cause) {
            super(context);
            this.cause = cause;
        }
    }
}
