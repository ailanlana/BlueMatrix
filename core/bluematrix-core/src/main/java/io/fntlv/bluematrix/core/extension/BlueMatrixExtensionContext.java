package io.fntlv.bluematrix.core.extension;

import lombok.Getter;

public final class BlueMatrixExtensionContext {
    @Getter
    private final String name;

    BlueMatrixExtensionContext(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        this.name = name;
    }
}
