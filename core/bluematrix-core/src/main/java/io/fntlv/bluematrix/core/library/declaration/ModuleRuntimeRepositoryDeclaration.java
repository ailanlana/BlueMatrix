package io.fntlv.bluematrix.core.library.declaration;

import io.fntlv.bluematrix.core.library.runtime.RuntimeLibraryNames;

import java.util.Objects;

public final class ModuleRuntimeRepositoryDeclaration {
    private final String moduleId;
    private final String repositoryUrl;

    public ModuleRuntimeRepositoryDeclaration(String moduleId, String repositoryUrl) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        this.moduleId = moduleId.trim();
        this.repositoryUrl = RuntimeLibraryNames.normalizeRepository(repositoryUrl);
    }

    public String moduleId() {
        return moduleId;
    }

    public String repositoryUrl() {
        return repositoryUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModuleRuntimeRepositoryDeclaration)) {
            return false;
        }
        ModuleRuntimeRepositoryDeclaration that = (ModuleRuntimeRepositoryDeclaration) o;
        return Objects.equals(moduleId, that.moduleId)
                && Objects.equals(repositoryUrl, that.repositoryUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleId, repositoryUrl);
    }
}
