package io.fntlv.bluematrix.loader.library;

import java.util.regex.Pattern;

public final class BlueLibraryFactory {
    private BlueLibraryFactory() {
    }

    public static BlueLibrary of(String coordinates) {
        if (coordinates == null || coordinates.trim().isEmpty()) {
            throw new IllegalArgumentException("coordinates cannot be blank");
        }

        String[] parts = coordinates.split(Pattern.quote(":"));
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Expected 'groupId:artifactId:version[:checksum]' but got: " + coordinates);
        }

        String checksum = parts.length > 3 ? parts[3] : null;
        return new BlueLibrary(parts[0], parts[1], parts[2], checksum);
    }
}
