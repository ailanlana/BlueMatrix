package io.fntlv.bluematrix.core.library;

final class RuntimeLibraryNames {
    private RuntimeLibraryNames() {
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    static String normalizeRepository(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("repositoryUrl cannot be blank");
        }
        return repositoryUrl.trim();
    }
}
