package io.fntlv.bluematrix.core.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a loadable module with configurable metadata.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {
    /*
     * Required fields
     */

    /**
     * Reverse-domain style unique identifier (e.g. "io.fntlv.quest").
     * Must be lowercase and globally unique.
     */
    String id();

    /** Human-readable display name for admin interfaces */
    String name();

    /*
     * Optional configurations
     */

    /** Follows semantic versioning (major.minor.patch) */
    String version() default "1.0.0";

    /** Brief description shown in module lists */
    String description() default "";

    /** Required module IDs that will be loaded before this module */
    String[] dependencies() default {};

    /** Soft module IDs that will be loaded before this module */
    String[] softDependencies() default {};

    /** Runtime libraries loaded before onLoad */
    String[] libraries() default {};

    /** Maven repositories used to resolve this module's runtime libraries */
    String[] repositories() default {};

    /** Determines initialization order */
    LoadOrder loadOrder() default LoadOrder.NORMAL;

    /** Auto-enable when no explicit config exists */
    boolean enableByDefault() default true;

    /** Packages scanned for module-owned metadata such as config classes and components */
    String[] scanPackages() default {};

    /**
     * Module initialization priority levels.
     * Higher priority modules initialize earlier.
     */
    enum LoadOrder {
        /** First to load, last to unload */
        HIGHEST,
        /** Early loading for core services */
        HIGH,
        /** Default loading phase */
        NORMAL,
        /** Late loading for optional features */
        LOW,
        /** Last to load, first to unload */
        LOWEST
    }
}
