package io.fntlv.bluematrix.core.module;

import lombok.Getter;

@Getter
public final class ModuleConditionOutcome {
    private static final ModuleConditionOutcome MATCH = new ModuleConditionOutcome(true, "core", "Matched");

    private final boolean match;
    private final String source;
    private final String message;

    private ModuleConditionOutcome(boolean match, String source, String message) {
        this.match = match;
        this.source = source;
        this.message = message;
    }

    public static ModuleConditionOutcome match() {
        return MATCH;
    }

    public static ModuleConditionOutcome noMatch(String source, String message) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("source cannot be blank");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message cannot be blank");
        }
        return new ModuleConditionOutcome(false, source, message);
    }

}
