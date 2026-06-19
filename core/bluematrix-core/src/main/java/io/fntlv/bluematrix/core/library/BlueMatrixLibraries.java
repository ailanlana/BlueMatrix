package io.fntlv.bluematrix.core.library;

import io.fntlv.bluematrix.loader.BlueLibraryManager;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import io.fntlv.bluematrix.loader.library.BlueLibraryFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlueMatrixLibraries {
    private static final String[] CORE_LIBRARIES = {
            "org.reflections:reflections:0.10.2",
            "org.javassist:javassist:3.28.0-GA",
            "com.google.code.findbugs:jsr305:3.0.2",
            "org.slf4j:slf4j-api:1.7.36"
    };

    private static final String[] LOGGING_LIBRARIES = {
            "org.slf4j:slf4j-api:1.7.36",
            "ch.qos.logback:logback-core:1.2.13",
            "ch.qos.logback:logback-classic:1.2.13"
    };

    private BlueMatrixLibraries() {
    }

    public static List<BlueLibrary> core() {
        return toLibraries(CORE_LIBRARIES);
    }

    public static List<BlueLibrary> logging() {
        return toLibraries(LOGGING_LIBRARIES);
    }

    public static List<BlueLibrary> framework() {
        Map<String, BlueLibrary> libraries = new LinkedHashMap<>();
        add(libraries, core());
        add(libraries, logging());
        return new ArrayList<>(libraries.values());
    }

    public static void loadCore(BlueLibraryManager manager) {
        load(manager, core());
    }

    public static void loadLogging(BlueLibraryManager manager) {
        load(manager, logging());
    }

    public static void loadFramework(BlueLibraryManager manager) {
        load(manager, framework());
    }

    private static void load(BlueLibraryManager manager, Collection<BlueLibrary> libraries) {
        manager.addMavenCentral();
        manager.loadLibraries(libraries);
    }

    private static void add(Map<String, BlueLibrary> target, Collection<BlueLibrary> libraries) {
        for (BlueLibrary library : libraries) {
            target.put(library.toString(), library);
        }
    }

    private static List<BlueLibrary> toLibraries(String... coordinates) {
        List<BlueLibrary> libraries = new ArrayList<>();
        for (String coordinate : coordinates) {
            libraries.add(BlueLibraryFactory.of(coordinate));
        }
        return libraries;
    }
}
