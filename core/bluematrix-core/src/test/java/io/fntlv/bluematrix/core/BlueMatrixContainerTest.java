package io.fntlv.bluematrix.core;

import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryScope;
import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.BlueMatrixContainerException;
import io.fntlv.bluematrix.core.bootstrap.BlueMatrixBootstrap;
import io.fntlv.bluematrix.core.bootstrap.BlueMatrixBootstrapPlan;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionException;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionLoader;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.instance.ModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.orchestration.DefaultModuleOrchestrator;
import io.fntlv.bluematrix.core.module.orchestration.ModuleOrchestrator;
import io.fntlv.bluematrix.core.module.registration.provider.JarModuleProvider;
import io.fntlv.bluematrix.loader.library.BlueLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueMatrixContainerTest {

    @TempDir
    File tempDir;

    @Test
    void builderRejectsMissingModuleProvider() {
        assertThrows(BlueMatrixContainerException.class, () -> BlueMatrixContainer.builder(tempDir).build());
    }

    @Test
    void registryIsNotModuleOrchestrator() {
        BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                .jarDirectory(tempDir)
                .build();

        assertFalse(blueMatrixContainer.getRegistry() instanceof ModuleOrchestrator);
        assertFalse(blueMatrixContainer.getRegistry() instanceof DefaultModuleOrchestrator);
    }

    @Test
    void exposesEventBus() {
        BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                .jarDirectory(tempDir)
                .build();

        assertNotNull(blueMatrixContainer.getEventBus());
    }

    @Test
    void exposesParameterResolversAndInstanceFactory() {
        BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                .jarDirectory(tempDir)
                .build();

        ModuleParameterResolverRegistry parameterResolvers = blueMatrixContainer.getParameterResolvers();
        ModuleInstanceFactory instanceFactory = blueMatrixContainer.getInstanceFactory();

        assertNotNull(parameterResolvers);
        assertNotNull(instanceFactory);
    }

    @Test
    void builderRegistersParameterResolver() {
        TestParameterResolver resolver = new TestParameterResolver();
        BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                .jarDirectory(tempDir)
                .parameterResolver(resolver)
                .build();

        assertTrue(blueMatrixContainer.getParameterResolvers().resolvers().contains(resolver));
    }

    @Test
    void builderRegistersEventListener() {
        CountingListener.receivedEvents = 0;
        BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                .jarDirectory(tempDir)
                .eventListener(new CountingListener())
                .build();

        blueMatrixContainer.getEventBus().publish(new ModuleLoadEvent.Pre(new ModuleContext(
                new TestModule(),
                TestModule.class.getAnnotation(ModuleInfo.class)
        )));

        assertEquals(1, CountingListener.receivedEvents);
    }

    @Test
    void builderExposesExtensionBootstrapInterface() {
        BlueMatrixContainer.Builder builder = BlueMatrixContainer.builder(tempDir);
        BlueMatrixExtensionBootstrap bootstrap = builder;

        assertSame(tempDir, bootstrap.dataFolder());
        assertSame(builder, bootstrap.eventListener(new CountingListener()));
    }

    @Test
    void builderDownloadsExtensionLibrary() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixContainer.Builder builder = BlueMatrixContainer.builder(tempDir)
                    .extensionLibrary("library", "com.example:example-lib:1.0.0")
                    .jarDirectory(tempDir);

            assertEquals(0, downloader.calls);

            builder.build();

            assertEquals(1, downloader.calls);
            assertEquals(tempDir, downloader.dataFolder);
            assertEquals(BlueMatrixLibraryScope.EXTENSION, downloader.scope);
            assertEquals("library", downloader.qualifier);
            assertEquals("com.example:example-lib:1.0.0", downloader.library.toString());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void builderSkipsDuplicateExtensionLibrary() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixContainer.Builder builder = BlueMatrixContainer.builder(tempDir)
                    .extensionLibrary("duplicate-library", "com.example:duplicate-lib:1.0.0")
                    .extensionLibrary("duplicate-library", "com.example:duplicate-lib:1.0.0")
                    .jarDirectory(tempDir);

            assertEquals(0, downloader.calls);

            builder.build();

            assertEquals(1, downloader.calls);
            assertEquals(BlueMatrixLibraryScope.EXTENSION, downloader.scope);
            assertEquals("duplicate-library", downloader.qualifier);
            assertEquals("com.example:duplicate-lib:1.0.0", downloader.library.toString());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void builderSkipsExtensionLibraryWhenPresenceClassExists() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixContainer.builder(tempDir)
                    .extensionLibrary("library", "com.example:example-lib:1.0.0", String.class.getName())
                    .jarDirectory(tempDir)
                    .build();

            assertEquals(0, downloader.calls);
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void builderDoesNotValidateExtensionLibraryNameFormat() {
        BlueMatrixContainer.builder(tempDir)
                .extensionLibrary("Invalid Name", "com.example:example-lib:1.0.0");
    }

    @Test
    void builderDownloadsAppLibrary() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixContainer.Builder builder = BlueMatrixContainer.builder(tempDir)
                    .appLibrary("com.example:app-lib:1.0.0")
                    .jarDirectory(tempDir);

            assertEquals(0, downloader.calls);

            builder.build();

            assertEquals(1, downloader.calls);
            assertEquals(tempDir, downloader.dataFolder);
            assertEquals(BlueMatrixLibraryScope.APP, downloader.scope);
            assertEquals("", downloader.qualifier);
            assertEquals("com.example:app-lib:1.0.0", downloader.library.toString());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void builderSkipsDuplicateAppLibrary() {
        RecordingDownloader downloader = new RecordingDownloader();
        BlueMatrixLibraryLoader.downloaderForTesting(downloader);
        try {
            BlueMatrixContainer.Builder builder = BlueMatrixContainer.builder(tempDir)
                    .appLibrary("com.example:app-lib:1.0.0")
                    .appLibrary("com.example:app-lib:1.0.0")
                    .jarDirectory(tempDir);

            assertEquals(0, downloader.calls);

            builder.build();

            assertEquals(1, downloader.calls);
            assertEquals(BlueMatrixLibraryScope.APP, downloader.scope);
            assertEquals("", downloader.qualifier);
            assertEquals("com.example:app-lib:1.0.0", downloader.library.toString());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void buildWrapsRuntimeLibraryDownloadFailure() {
        RuntimeException failure = new RuntimeException("download failed");
        BlueMatrixLibraryLoader.downloaderForTesting((bootstrap, dataFolder, classLoader, scope, qualifier, library) -> {
            throw failure;
        });
        try {
            BlueMatrixContainer.Builder builder = BlueMatrixContainer.builder(tempDir)
                    .appLibrary("com.example:broken-lib:1.0.0")
                    .jarDirectory(tempDir);

            BlueMatrixContainerException exception = assertThrows(BlueMatrixContainerException.class, builder::build);

            assertEquals(failure, exception.getCause());
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
        }
    }

    @Test
    void extensionLoadInstantiatesExtension() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("constructor-failing", ConstructorFailingExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> new BlueMatrixExtensionLoader().load());

            assertExceptionMessageContainsDeclaration(exception, ConstructorFailingExtension.class.getName());
            assertTrue(exception.getMessage().contains("constructor threw an exception"));
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void buildDoesNotLaunchExtensionsWhenLibraryDownloadFails() throws Exception {
        MarkerExtension.constructed = false;
        MarkerExtension.launched = false;
        MarkerExtension.container = null;
        RuntimeException failure = new RuntimeException("download failed");
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("marker", MarkerExtension.class.getName());
        BlueMatrixLibraryLoader.downloaderForTesting((bootstrap, dataFolder, classLoader, scope, qualifier, library) -> {
            throw failure;
        });
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixContainer.Builder builder = BlueMatrixContainer.builder(tempDir)
                    .appLibrary("com.example:broken-lib:1.0.0")
                    .jarDirectory(tempDir);

            BlueMatrixContainerException exception = assertThrows(BlueMatrixContainerException.class, builder::build);

            assertEquals(failure, exception.getCause());
            assertTrue(MarkerExtension.constructed);
            assertFalse(MarkerExtension.launched);
            assertEquals(null, MarkerExtension.container);
        } finally {
            BlueMatrixLibraryLoader.downloaderForTesting(null);
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void buildDoesNotLaunchExtensionsWhenModuleProviderIsMissing() throws Exception {
        MarkerExtension.constructed = false;
        MarkerExtension.launched = false;
        MarkerExtension.container = null;
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("marker", MarkerExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixContainerException exception = assertThrows(BlueMatrixContainerException.class,
                    () -> BlueMatrixContainer.builder(tempDir).build());

            assertTrue(exception.getMessage().contains("At least one module provider is required"));
            assertTrue(MarkerExtension.constructed);
            assertFalse(MarkerExtension.launched);
            assertEquals(null, MarkerExtension.container);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void buildLaunchesExtensionsAfterContainerIsCreated() throws Exception {
        MarkerExtension.constructed = false;
        MarkerExtension.launched = false;
        MarkerExtension.container = null;
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("marker", MarkerExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                    .jarDirectory(tempDir)
                    .build();

            assertTrue(MarkerExtension.constructed);
            assertTrue(MarkerExtension.launched);
            assertEquals(blueMatrixContainer, MarkerExtension.container);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void bootstrapStartReturnsLaunchedContainer() throws Exception {
        MarkerExtension.constructed = false;
        MarkerExtension.launched = false;
        MarkerExtension.container = null;
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("marker", MarkerExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);
            BlueMatrixBootstrapPlan plan = new BlueMatrixBootstrapPlan(
                    tempDir,
                    extensionClassLoader,
                    new BlueMatrixLibraryLoader(tempDir, extensionClassLoader)
            );
            plan.addModuleProvider(new JarModuleProvider(tempDir, extensionClassLoader));

            BlueMatrixContainer blueMatrixContainer = new BlueMatrixBootstrap().start(plan);

            assertNotNull(blueMatrixContainer.getEventBus());
            assertTrue(blueMatrixContainer.getExtension(MarkerExtension.class).isPresent());
            assertTrue(MarkerExtension.launched);
            assertEquals(blueMatrixContainer, MarkerExtension.container);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void exposesExtensionByClass() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("marker", MarkerExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                    .jarDirectory(tempDir)
                    .build();

            assertTrue(blueMatrixContainer.getExtension(MarkerExtension.class).isPresent());
            assertTrue(blueMatrixContainer.getExtension(BlueMatrixExtension.class).isPresent());
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void returnsEmptyWhenExtensionIsMissing() {
        BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                .jarDirectory(tempDir)
                .build();

        assertFalse(blueMatrixContainer.getExtension(MarkerExtension.class).isPresent());
    }

    @Test
    void rejectsNullExtensionClass() {
        BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                .jarDirectory(tempDir)
                .build();

        assertThrows(IllegalArgumentException.class, () -> blueMatrixContainer.getExtension(null));
    }

    @Test
    void extensionLoaderAppliesExtensionsFromMetadataFile() throws Exception {
        CountingListener.receivedEvents = 0;
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("counting", CountingExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);
            BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                    .jarDirectory(tempDir)
                    .build();

            blueMatrixContainer.getEventBus().publish(new ModuleLoadEvent.Pre(new ModuleContext(
                    new TestModule(),
                    TestModule.class.getAnnotation(ModuleInfo.class)
            )));

            assertEquals(1, CountingListener.receivedEvents);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderAppliesMultipleExtensionsFromMetadataFiles() throws Exception {
        CountingListener.receivedEvents = 0;
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader(
                metadataRoot("extensions", "counting", "class=" + CountingExtension.class.getName() + "\n"),
                metadataRoot("extensions", "counting-second", "class=" + CountingExtension.class.getName() + "\n")
        );
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);
            BlueMatrixContainer blueMatrixContainer = BlueMatrixContainer.builder(tempDir)
                    .jarDirectory(tempDir)
                    .build();

            blueMatrixContainer.getEventBus().publish(new ModuleLoadEvent.Pre(new ModuleContext(
                    new TestModule(),
                    TestModule.class.getAnnotation(ModuleInfo.class)
            )));

            assertEquals(2, CountingListener.receivedEvents);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderRejectsNonExtensionClass() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("string", String.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class, () -> BlueMatrixContainer.builder(tempDir)
                    .jarDirectory(tempDir)
                    .build());

            assertExceptionMessageContainsDeclaration(exception, String.class.getName());
            assertTrue(exception.getMessage().contains("must implement"));
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderReportsMissingExtensionClass() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        String extensionClassName = "io.fntlv.bluematrix.core.MissingExtension";
        URLClassLoader extensionClassLoader = extensionClassLoader("missing", extensionClassName);
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> BlueMatrixContainer.builder(tempDir)
                            .jarDirectory(tempDir)
                            .build());

            assertExceptionMessageContainsDeclaration(exception, extensionClassName);
            assertTrue(exception.getMessage().contains("class not found"));
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderReportsMissingNoArgumentConstructor() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("constructor-argument", ConstructorArgumentExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> BlueMatrixContainer.builder(tempDir)
                            .jarDirectory(tempDir)
                            .build());

            assertExceptionMessageContainsDeclaration(exception, ConstructorArgumentExtension.class.getName());
            assertTrue(exception.getMessage().contains("no-argument constructor"));
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderReportsConstructorFailure() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("constructor-failing", ConstructorFailingExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> BlueMatrixContainer.builder(tempDir)
                            .jarDirectory(tempDir)
                            .build());

            assertExceptionMessageContainsDeclaration(exception, ConstructorFailingExtension.class.getName());
            assertTrue(exception.getMessage().contains("constructor threw an exception"));
            assertNotNull(exception.getCause());
            assertEquals("constructor failed", exception.getCause().getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderWrapsApplyFailure() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("failing", FailingExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> BlueMatrixContainer.builder(tempDir)
                            .jarDirectory(tempDir)
                            .build());

            assertNotNull(exception.getCause());
            assertEquals("apply failed", exception.getCause().getMessage());
            assertExceptionMessageContainsDeclaration(exception, FailingExtension.class.getName());
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderWrapsLaunchFailure() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader("launch-failing", LaunchFailingExtension.class.getName());
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> BlueMatrixContainer.builder(tempDir)
                            .jarDirectory(tempDir)
                            .build());

            assertNotNull(exception.getCause());
            assertEquals("launch failed", exception.getCause().getMessage());
            assertExceptionMessageContainsDeclaration(exception, LaunchFailingExtension.class.getName());
            assertTrue(exception.getMessage().contains("launch"));
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderRejectsMissingMetadataClass() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader(metadataRoot("extension", "counting", "\n"));
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> BlueMatrixContainer.builder(tempDir)
                            .jarDirectory(tempDir)
                            .build());

            assertTrue(exception.getMessage().contains("missing 'class'"));
            assertTrue(exception.getMessage().contains("counting.properties"));
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderRejectsInvalidMetadataName() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader(metadataRoot(
                "extension",
                "Invalid Name",
                "class=" + CountingExtension.class.getName() + "\n"
        ));
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> BlueMatrixContainer.builder(tempDir)
                            .jarDirectory(tempDir)
                            .build());

            assertTrue(exception.getMessage().contains("must match"));
            assertTrue(exception.getMessage().contains("Invalid Name"));
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void extensionLoaderRejectsDuplicateMetadataName() throws Exception {
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader extensionClassLoader = extensionClassLoader(
                metadataRoot("first", "duplicate", "class=" + CountingExtension.class.getName() + "\n"),
                metadataRoot("second", "duplicate", "class=" + FailingExtension.class.getName() + "\n")
        );
        try {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);

            BlueMatrixExtensionException exception = assertThrows(BlueMatrixExtensionException.class,
                    () -> BlueMatrixContainer.builder(tempDir)
                            .jarDirectory(tempDir)
                            .build());

            assertTrue(exception.getMessage().contains("Duplicate BlueMatrix extension name"));
            assertTrue(exception.getMessage().contains("duplicate"));
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            extensionClassLoader.close();
        }
    }

    @Test
    void builderCannotBeReused() {
        BlueMatrixContainer.Builder builder = BlueMatrixContainer.builder(tempDir)
                .jarDirectory(tempDir);

        builder.build();

        BlueMatrixContainerException exception = assertThrows(BlueMatrixContainerException.class, builder::build);
        assertTrue(exception.getMessage().contains("cannot be reused"));
    }

    private void assertExceptionMessageContainsDeclaration(BlueMatrixExtensionException exception, String extensionClassName) {
        assertTrue(exception.getMessage().contains(extensionClassName));
        assertTrue(exception.getMessage().contains(".properties"));
    }

    private URLClassLoader extensionClassLoader(String extensionName, String extensionClassName) throws Exception {
        return extensionClassLoader(metadataRoot("extension", extensionName, "class=" + extensionClassName + "\n"));
    }

    private File metadataRoot(String directoryName, String extensionName, String properties) throws Exception {
        File root = new File(tempDir, directoryName);
        File metadataFile = new File(root, "META-INF/bluematrix/extensions/" + extensionName + ".properties");
        Files.createDirectories(metadataFile.getParentFile().toPath());
        Files.write(metadataFile.toPath(), properties.getBytes(StandardCharsets.UTF_8));
        return root;
    }

    private URLClassLoader extensionClassLoader(File... roots) throws Exception {
        URL[] urls = new URL[roots.length];
        for (int i = 0; i < roots.length; i++) {
            urls[i] = roots[i].toURI().toURL();
        }
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    public static final class CountingExtension implements BlueMatrixExtension {
        @Override
        public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
            bootstrap.eventListener(new CountingListener());
        }
    }

    public static final class FailingExtension implements BlueMatrixExtension {
        @Override
        public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
            throw new IllegalStateException("apply failed");
        }
    }

    public static final class LaunchFailingExtension implements BlueMatrixExtension {
        @Override
        public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        }

        @Override
        public void launch(BlueMatrixContainer container, BlueMatrixExtensionContext context) {
            throw new IllegalStateException("launch failed");
        }
    }

    public static final class ConstructorArgumentExtension implements BlueMatrixExtension {
        public ConstructorArgumentExtension(String value) {
        }

        @Override
        public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        }
    }

    public static final class ConstructorFailingExtension implements BlueMatrixExtension {
        public ConstructorFailingExtension() {
            throw new IllegalStateException("constructor failed");
        }

        @Override
        public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        }
    }

    public static final class MarkerExtension implements BlueMatrixExtension {
        private static boolean constructed;
        private static boolean launched;
        private static BlueMatrixContainer container;

        public MarkerExtension() {
            constructed = true;
        }

        @Override
        public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        }

        @Override
        public void launch(BlueMatrixContainer container, BlueMatrixExtensionContext context) {
            launched = true;
            MarkerExtension.container = container;
        }
    }

    public static final class CountingListener {
        private static int receivedEvents;

        @ModuleEventListener
        public void onLoadPre(ModuleLoadEvent.Pre event) {
            receivedEvents++;
        }
    }

    private static final class TestParameterResolver implements ModuleParameterResolver {
        @Override
        public boolean supports(Class<?> parameterType, io.fntlv.bluematrix.core.module.instance.InjectContext context) {
            return false;
        }

        @Override
        public Object resolve(Class<?> parameterType, io.fntlv.bluematrix.core.module.instance.InjectContext context) {
            return null;
        }
    }

    private static final class RecordingDownloader implements BlueMatrixLibraryLoader.Downloader {
        private int calls;
        private File dataFolder;
        private BlueMatrixLibraryScope scope;
        private String qualifier;
        private BlueLibrary library;

        @Override
        public void download(
                BlueMatrixLibraryLoader bootstrap,
                File dataFolder,
                ClassLoader classLoader,
                BlueMatrixLibraryScope scope,
                String qualifier,
                BlueLibrary library
        ) {
            calls++;
            this.dataFolder = dataFolder;
            this.scope = scope;
            this.qualifier = qualifier == null ? "" : qualifier;
            this.library = library;
        }
    }

    @ModuleInfo(id = "test", name = "Test")
    private static final class TestModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }
}
