package io.fntlv.bluematrix.loader;

import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.classloader.URLClassLoaderHelper;
import net.byteflux.libby.logging.adapters.JDKLogAdapter;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

public final class BlueClassLoaderSupport {
    private static final LibraryManager LIBRARY_MANAGER = new ClassPathLibraryManager();

    private BlueClassLoaderSupport() {
    }

    public static URLClassLoader ensureUrlClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        if (classLoader instanceof URLClassLoader) {
            return (URLClassLoader) classLoader;
        }
        return new URLClassLoader(new URL[0], classLoader);
    }

    public static URLClassLoader requireUrlClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        if (!(classLoader instanceof URLClassLoader)) {
            throw new BlueMatrixLoaderException("Classpath appending requires a URLClassLoader target");
        }
        return (URLClassLoader) classLoader;
    }

    public static void addUrl(ClassLoader classLoader, URL url) {
        Objects.requireNonNull(url, "url");
        new URLClassLoaderHelper(requireUrlClassLoader(classLoader), LIBRARY_MANAGER).addToClasspath(url);
    }

    private static final class ClassPathLibraryManager extends LibraryManager {
        private ClassPathLibraryManager() {
            super(
                    new JDKLogAdapter(Logger.getLogger("BlueClassLoaderSupport")),
                    java.nio.file.Paths.get("."),
                    "classpath"
            );
        }

        @Override
        protected void addToClasspath(Path file) {
            throw new UnsupportedOperationException("Use BlueClassLoaderSupport.addUrl instead");
        }
    }
}
