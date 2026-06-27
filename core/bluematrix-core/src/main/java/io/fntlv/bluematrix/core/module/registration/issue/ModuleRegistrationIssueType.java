package io.fntlv.bluematrix.core.module.registration.issue;

public enum ModuleRegistrationIssueType {
    /**
     * Multiple discovered module candidates declared the same module id.
     */
    DUPLICATE_MODULE_ID,

    /**
     * A module candidate declared a required dependency that is not available.
     */
    MISSING_REQUIRED_DEPENDENCY,

    /**
     * A module candidate is part of a dependency cycle and cannot be ordered safely.
     */
    CIRCULAR_DEPENDENCY,

    /**
     * A module candidate was dependency-ready but failed during instance creation.
     */
    INSTANTIATION_FAILED,

    /**
     * A module candidate could not load its declared runtime libraries.
     */
    RUNTIME_LIBRARY_LOAD_FAILED
}
