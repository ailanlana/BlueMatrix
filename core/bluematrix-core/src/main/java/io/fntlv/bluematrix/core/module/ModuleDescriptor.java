package io.fntlv.bluematrix.core.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ModuleDescriptor {
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String[] dependencies;
    private final String[] softDependencies;
    private final String[] libraries;
    private final String[] repositories;
    private final ModuleInfo.LoadOrder loadOrder;
    private final boolean enableByDefault;
    private final String[] scanPackages;

    private ModuleDescriptor(String id,
                             String name,
                             String version,
                             String description,
                             String[] dependencies,
                             String[] softDependencies,
                             String[] libraries,
                             String[] repositories,
                             ModuleInfo.LoadOrder loadOrder,
                             boolean enableByDefault,
                             String[] scanPackages) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.dependencies = copy(dependencies);
        this.softDependencies = copy(softDependencies);
        this.libraries = copy(libraries);
        this.repositories = copy(repositories);
        this.loadOrder = loadOrder;
        this.enableByDefault = enableByDefault;
        this.scanPackages = copy(scanPackages);
    }

    public static ModuleDescriptor from(Class<? extends Module> moduleClass, ModuleInfo moduleInfo) {
        if (moduleClass == null) {
            throw new IllegalArgumentException("moduleClass cannot be null");
        }
        if (moduleInfo == null) {
            throw new IllegalArgumentException("moduleInfo cannot be null");
        }
        return new ModuleDescriptor(
                moduleInfo.id(),
                moduleInfo.name(),
                moduleInfo.version(),
                moduleInfo.description(),
                moduleInfo.dependencies(),
                moduleInfo.softDependencies(),
                moduleInfo.libraries(),
                moduleInfo.repositories(),
                moduleInfo.loadOrder(),
                moduleInfo.enableByDefault(),
                resolveScanPackages(moduleClass, moduleInfo)
        );
    }

    public String id() {
        return id;
    }

    public String getId() {
        return id();
    }

    public String name() {
        return name;
    }

    public String getName() {
        return name();
    }

    public String version() {
        return version;
    }

    public String getVersion() {
        return version();
    }

    public String description() {
        return description;
    }

    public String getDescription() {
        return description();
    }

    public String[] dependencies() {
        return copy(dependencies);
    }

    public String[] getDependencies() {
        return dependencies();
    }

    public String[] softDependencies() {
        return copy(softDependencies);
    }

    public String[] getSoftDependencies() {
        return softDependencies();
    }

    public String[] libraries() {
        return copy(libraries);
    }

    public String[] getLibraries() {
        return libraries();
    }

    public String[] repositories() {
        return copy(repositories);
    }

    public String[] getRepositories() {
        return repositories();
    }

    public ModuleInfo.LoadOrder loadOrder() {
        return loadOrder;
    }

    public ModuleInfo.LoadOrder getLoadOrder() {
        return loadOrder();
    }

    public boolean enableByDefault() {
        return enableByDefault;
    }

    public boolean isEnableByDefault() {
        return enableByDefault();
    }

    public String[] scanPackages() {
        return copy(scanPackages);
    }

    public String[] getScanPackages() {
        return scanPackages();
    }

    private static String[] resolveScanPackages(Class<? extends Module> moduleClass, ModuleInfo moduleInfo) {
        List<String> packages = new ArrayList<>();
        for (String scanPackage : moduleInfo.scanPackages()) {
            if (scanPackage != null && !scanPackage.trim().isEmpty()) {
                packages.add(scanPackage);
            }
        }
        if (packages.isEmpty()) {
            packages.add(moduleClass.getPackage().getName());
        }
        return packages.toArray(new String[0]);
    }

    private static String[] copy(String[] values) {
        if (values == null) {
            return new String[0];
        }
        return Arrays.copyOf(values, values.length);
    }
}
