package io.fntlv.bluematrix.loader;

import io.fntlv.bluematrix.loader.BlueMatrixLoaderException;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.classloader.URLClassLoaderHelper;
import net.byteflux.libby.logging.adapters.JDKLogAdapter;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlueLibraryManager {
    private static final int DOWNLOAD_THREADS = 3;

    private final Logger logger;
    private final DelegateLibraryManager delegate;

    public BlueLibraryManager(String name, File rootFolder, String libsFolderName) {
        this(name, rootFolder, libsFolderName, BlueLibraryManager.class.getClassLoader());
    }

    public BlueLibraryManager(String name, File rootFolder, String libsFolderName, ClassLoader classLoader) {
        validate(name, rootFolder, libsFolderName, classLoader);
        this.logger = Logger.getLogger("BlueLibraryManager_" + name);
        this.delegate = new DelegateLibraryManager(logger, rootFolder, libsFolderName, (URLClassLoader) classLoader);
    }

    public void addMavenCentral() {
        delegate.addMavenCentral();
    }

    public void addRepository(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("repositoryUrl cannot be blank");
        }
        delegate.addRepository(repositoryUrl.trim());
    }

    public void loadLibrary(BlueLibrary library) {
        Objects.requireNonNull(library, "library");
        loadSingleLibrary(library);
    }

    public void loadLibraries(Collection<BlueLibrary> libraries) {
        Objects.requireNonNull(libraries, "libraries");
        if (libraries.isEmpty()) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(libraries.size());
        ExecutorService executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS);

        for (BlueLibrary library : libraries) {
            executor.execute(() -> {
                try {
                    loadLibrary(library);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "Unable to load dependency " + library + ".", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }

    protected void loadSingleLibrary(BlueLibrary library) {
        delegate.loadLibrary(toLibbyLibrary(library));
    }

    private Library toLibbyLibrary(BlueLibrary library) {
        Library.Builder builder = Library.builder()
                .groupId(library.getGroupId())
                .artifactId(library.getArtifactId())
                .version(library.getVersion());

        if (library.hasChecksum()) {
            builder.checksum(library.getChecksum());
        }
        for (BlueLibrary.Relocation relocation : library.getRelocations()) {
            builder.relocate(relocation.getPattern(), relocation.getRelocatedPattern());
        }

        return builder.build();
    }

    private void validate(String name, File rootFolder, String libsFolderName, ClassLoader classLoader) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (rootFolder == null) {
            throw new IllegalArgumentException("rootFolder cannot be null");
        }
        if (libsFolderName == null || libsFolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("libsFolderName cannot be blank");
        }
        if (!(classLoader instanceof URLClassLoader)) {
            throw new BlueMatrixLoaderException("Library loading requires a URLClassLoader target");
        }
    }

    private static final class DelegateLibraryManager extends LibraryManager {
        private final URLClassLoaderHelper classLoaderHelper;

        private DelegateLibraryManager(Logger logger, File rootFolder, String libsFolderName, URLClassLoader classLoader) {
            super(new JDKLogAdapter(logger), rootFolder.toPath(), libsFolderName);
            this.classLoaderHelper = new URLClassLoaderHelper(classLoader, this);
        }

        @Override
        protected void addToClasspath(Path file) {
            classLoaderHelper.addToClasspath(file);
        }
    }
}
