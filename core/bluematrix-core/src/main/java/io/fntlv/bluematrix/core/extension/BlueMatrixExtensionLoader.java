package io.fntlv.bluematrix.core.extension;

import io.fntlv.bluematrix.core.BlueMatrixContainer;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

public final class BlueMatrixExtensionLoader {
    private static final String EXTENSION_DIRECTORY = "META-INF/bluematrix/extensions";
    private static final String EXTENSION_FILE_SUFFIX = ".properties";
    private static final Pattern EXTENSION_NAME_PATTERN = Pattern.compile("[a-z0-9_-]+");

    private final ClassLoader classLoader;
    private final List<LoadedExtension> extensions = new ArrayList<>();

    public BlueMatrixExtensionLoader() {
        this(resolveClassLoader());
    }

    public BlueMatrixExtensionLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null");
        }
        this.classLoader = classLoader;
    }

    public BlueMatrixExtensionLoader load() {
        extensions.clear();
        for (ExtensionDeclaration declaration : discoverDeclarations(classLoader)) {
            extensions.add(new LoadedExtension(declaration, instantiateExtension(declaration)));
        }
        return this;
    }

    public void apply(BlueMatrixExtensionBootstrap bootstrap) {
        for (LoadedExtension loadedExtension : extensions) {
            try {
                loadedExtension.extension.apply(bootstrap, loadedExtension.context());
            } catch (Exception e) {
                throw new BlueMatrixExtensionException(
                        "Failed to apply BlueMatrix extension: " + loadedExtension.declaration.describe(),
                        e
                );
            }
        }
    }

    public void launch(BlueMatrixContainer container) {
        for (LoadedExtension loadedExtension : extensions) {
            try {
                loadedExtension.extension.launch(container, loadedExtension.context());
            } catch (Exception e) {
                throw new BlueMatrixExtensionException(
                        "Failed to launch BlueMatrix extension: " + loadedExtension.declaration.describe(),
                        e
                );
            }
        }
    }

    private static ClassLoader resolveClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = BlueMatrixExtensionLoader.class.getClassLoader();
        }
        return classLoader;
    }

    private static List<ExtensionDeclaration> discoverDeclarations(ClassLoader classLoader) {
        List<ExtensionDeclaration> declarations = new ArrayList<>();
        Set<String> names = new HashSet<>();
        try {
            for (MetadataResource resource : discoverMetadataResources(classLoader)) {
                ExtensionDeclaration declaration = readDeclaration(resource.path, resource.url);
                if (!names.add(declaration.name)) {
                    throw new BlueMatrixExtensionException("Duplicate BlueMatrix extension name: "
                            + declaration.name + " [source=" + resource.url + "]");
                }
                declarations.add(declaration);
            }
            return declarations;
        } catch (IOException e) {
            throw new BlueMatrixExtensionException("Failed to load BlueMatrix extension declarations", e);
        }
    }

    private static List<MetadataResource> discoverMetadataResources(ClassLoader classLoader) throws IOException {
        List<MetadataResource> metadataResources = new ArrayList<>();
        Enumeration<URL> directories = classLoader.getResources(EXTENSION_DIRECTORY);
        while (directories.hasMoreElements()) {
            URL directory = directories.nextElement();
            String protocol = directory.getProtocol();
            if ("file".equals(protocol)) {
                discoverFileMetadataResources(directory, metadataResources);
            } else if ("jar".equals(protocol)) {
                discoverJarMetadataResources(directory, metadataResources);
            }
        }
        Collections.sort(metadataResources, new Comparator<MetadataResource>() {
            @Override
            public int compare(MetadataResource left, MetadataResource right) {
                int byPath = left.path.compareTo(right.path);
                if (byPath != 0) {
                    return byPath;
                }
                return left.url.toExternalForm().compareTo(right.url.toExternalForm());
            }
        });
        return metadataResources;
    }

    private static void discoverFileMetadataResources(URL directory, List<MetadataResource> metadataResources) throws IOException {
        File file = new File(URLDecoder.decode(directory.getPath(), "UTF-8"));
        File[] children = file.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && child.getName().endsWith(EXTENSION_FILE_SUFFIX)) {
                metadataResources.add(new MetadataResource(
                        EXTENSION_DIRECTORY + "/" + child.getName(),
                        child.toURI().toURL()
                ));
            }
        }
    }

    private static void discoverJarMetadataResources(URL directory, List<MetadataResource> metadataResources) throws IOException {
        JarURLConnection connection = (JarURLConnection) directory.openConnection();
        JarFile jarFile = connection.getJarFile();
        String prefix = EXTENSION_DIRECTORY + "/";
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory()
                    && name.startsWith(prefix)
                    && name.endsWith(EXTENSION_FILE_SUFFIX)
                    && name.indexOf('/', prefix.length()) < 0) {
                metadataResources.add(new MetadataResource(
                        name,
                        new URL("jar:" + connection.getJarFileURL().toExternalForm() + "!/" + name)
                ));
            }
        }
    }

    private static ExtensionDeclaration readDeclaration(String metadataPath, URL resource) {
        try (InputStream inputStream = resource.openStream()) {
            Properties properties = new Properties();
            properties.load(inputStream);
            String name = extensionNameFromPath(metadataPath, resource);
            String className = requireProperty(properties, "class", resource);
            validateName(name, resource);
            return new ExtensionDeclaration(name, className, resource);
        } catch (IOException e) {
            throw new BlueMatrixExtensionException(
                    "Failed to read BlueMatrix extension declarations from " + resource,
                    e
            );
        }
    }

    private static String extensionNameFromPath(String metadataPath, URL resource) {
        int slash = metadataPath.lastIndexOf('/');
        String fileName = slash >= 0 ? metadataPath.substring(slash + 1) : metadataPath;
        if (!fileName.endsWith(EXTENSION_FILE_SUFFIX)) {
            throw new BlueMatrixExtensionException("BlueMatrix extension metadata file must end with "
                    + EXTENSION_FILE_SUFFIX + ": " + metadataPath + " [source=" + resource + "]");
        }
        return fileName.substring(0, fileName.length() - EXTENSION_FILE_SUFFIX.length());
    }

    private static String requireProperty(Properties properties, String key, URL resource) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw missingProperty(key, resource);
        }
        return value.trim();
    }

    private static BlueMatrixExtensionException missingProperty(String key, URL resource) {
        return new BlueMatrixExtensionException("BlueMatrix extension metadata is missing '" + key
                + "' [source=" + resource + "]");
    }

    private static void validateName(String name, URL resource) {
        if (!EXTENSION_NAME_PATTERN.matcher(name).matches()) {
            throw new BlueMatrixExtensionException("BlueMatrix extension name must match [a-z0-9_-]+: "
                    + name + " [source=" + resource + "]");
        }
    }

    private BlueMatrixExtension instantiateExtension(ExtensionDeclaration declaration) {
        try {
            Class<?> extensionClass = Class.forName(declaration.className, true, classLoader);
            if (!BlueMatrixExtension.class.isAssignableFrom(extensionClass)) {
                throw new BlueMatrixExtensionException("BlueMatrix extension must implement "
                        + BlueMatrixExtension.class.getName() + ": " + declaration.describe());
            }
            return (BlueMatrixExtension) extensionClass.getDeclaredConstructor().newInstance();
        } catch (BlueMatrixExtensionException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new BlueMatrixExtensionException("Extension class not found: " + declaration.describe(), e);
        } catch (NoSuchMethodException e) {
            throw new BlueMatrixExtensionException("Extension must declare a no-argument constructor: "
                    + declaration.describe(), e);
        } catch (IllegalAccessException e) {
            throw new BlueMatrixExtensionException("Extension no-argument constructor is not accessible: "
                    + declaration.describe(), e);
        } catch (InstantiationException e) {
            throw new BlueMatrixExtensionException("Extension class cannot be instantiated: "
                    + declaration.describe(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new BlueMatrixExtensionException("Extension constructor threw an exception: "
                    + declaration.describe(), cause);
        } catch (LinkageError e) {
            throw new BlueMatrixExtensionException("Extension class linkage failed: "
                    + declaration.describe(), e);
        } catch (Exception e) {
            throw new BlueMatrixExtensionException("Failed to create BlueMatrix extension: " + declaration.describe(), e);
        }
    }

    private static final class ExtensionDeclaration {
        private final String name;
        private final String className;
        private final URL source;

        private ExtensionDeclaration(String name, String className, URL source) {
            this.name = name;
            this.className = className;
            this.source = source;
        }

        private String describe() {
            return name + " (" + className + ") [source=" + source + "]";
        }
    }

    private static final class MetadataResource {
        private final String path;
        private final URL url;

        private MetadataResource(String path, URL url) {
            this.path = path;
            this.url = url;
        }
    }

    private static final class LoadedExtension {
        private final ExtensionDeclaration declaration;
        private final BlueMatrixExtension extension;

        private LoadedExtension(ExtensionDeclaration declaration, BlueMatrixExtension extension) {
            this.declaration = declaration;
            this.extension = extension;
        }

        private BlueMatrixExtensionContext context() {
            return new BlueMatrixExtensionContext(declaration.name);
        }
    }
}
