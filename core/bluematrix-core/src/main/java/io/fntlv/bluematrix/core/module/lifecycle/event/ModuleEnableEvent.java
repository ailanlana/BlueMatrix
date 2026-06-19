package io.fntlv.bluematrix.core.module.lifecycle.event;

import io.fntlv.bluematrix.core.module.ModuleConditionOutcome;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.event.ModuleEvent;
import lombok.Getter;

@Getter
public abstract class ModuleEnableEvent implements ModuleEvent {
    private final ModuleContext context;

    private ModuleEnableEvent(ModuleContext context) {
        this.context = context;
    }

    public static final class Pre extends ModuleEnableEvent {
        private ModuleConditionOutcome cancelOutcome;
        private String errorSource;
        private String errorMessage;
        private Throwable errorCause;

        public Pre(ModuleContext context) {
            super(context);
        }

        public void cancel(ModuleConditionOutcome outcome) {
            if (outcome == null) {
                throw new IllegalArgumentException("outcome cannot be null");
            }
            if (outcome.isMatch()) {
                throw new IllegalArgumentException("cancel requires a no-match outcome");
            }
            this.cancelOutcome = outcome;
        }

        public boolean isCancelled() {
            return cancelOutcome != null;
        }

        public ModuleConditionOutcome getCancelOutcome() {
            return cancelOutcome;
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

        public String getErrorSource() {
            return errorSource;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Throwable getErrorCause() {
            return errorCause;
        }
    }

    public static final class Post extends ModuleEnableEvent {
        public Post(ModuleContext context) {
            super(context);
        }
    }

    @Getter
    public static final class Skipped extends ModuleEnableEvent {
        private final ModuleConditionOutcome outcome;

        public Skipped(ModuleContext context, ModuleConditionOutcome outcome) {
            super(context);
            this.outcome = outcome;
        }
    }

    @Getter
    public static final class Failed extends ModuleEnableEvent {
        private final Throwable cause;

        public Failed(ModuleContext context, Throwable cause) {
            super(context);
            this.cause = cause;
        }
    }
}
