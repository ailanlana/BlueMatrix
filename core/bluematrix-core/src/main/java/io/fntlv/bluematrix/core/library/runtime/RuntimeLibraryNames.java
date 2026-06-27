package io.fntlv.bluematrix.core.library.runtime;

public final class RuntimeLibraryNames {
    private RuntimeLibraryNames() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static String normalizeRepository(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("repositoryUrl cannot be blank");
        }
        return repositoryUrl.trim();
    }
}
