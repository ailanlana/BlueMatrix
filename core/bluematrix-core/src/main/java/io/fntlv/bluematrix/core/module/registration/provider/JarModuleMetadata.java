package io.fntlv.bluematrix.core.module.registration.provider;

final class JarModuleMetadata {
    private final String className;
    private final String id;
    private final String name;
    private final String[] repositories;
    private final String[] libraries;

    JarModuleMetadata(String className, String id, String name, String[] repositories, String[] libraries) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("className cannot be blank");
        }
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        this.className = className;
        this.id = id;
        this.name = name;
        this.repositories = repositories == null ? new String[0] : repositories.clone();
        this.libraries = libraries == null ? new String[0] : libraries.clone();
    }

    String className() {
        return className;
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    String[] repositories() {
        return repositories.clone();
    }

    String[] libraries() {
        return libraries.clone();
    }
}
