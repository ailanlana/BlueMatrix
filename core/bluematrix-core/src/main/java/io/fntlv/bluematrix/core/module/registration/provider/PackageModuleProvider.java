package io.fntlv.bluematrix.core.module.registration.provider;

import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoadResult;
import io.fntlv.bluematrix.core.module.registration.library.ModuleRuntimeLibraryLoader;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleDescriptor;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegistrationStageResult;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssue;
import io.fntlv.bluematrix.core.module.registration.issue.ModuleRegistrationIssues;
import io.fntlv.bluematrix.core.module.registration.issue.issues.RuntimeLibraryLoadFailedIssue;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PackageModuleProvider implements ModuleProvider {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(PackageModuleProvider.class);

    private final String packagePath;
    private final ModuleRuntimeLibraryLoader runtimeLibraryLoader;

    public PackageModuleProvider(String packagePath){
        this(packagePath, null);
    }

    public PackageModuleProvider(String packagePath, ModuleRuntimeLibraryLoader runtimeLibraryLoader) {
        this.packagePath = packagePath;
        this.runtimeLibraryLoader = runtimeLibraryLoader;
    }

    @Override
    public ModuleRegistrationStageResult<ModuleCandidate> discoverModules() {
        List<ModuleCandidate> discoveredModules = new ArrayList<>();
        List<ModuleRegistrationIssue> issues = new ArrayList<>();
        List<Class<? extends Module>> classes;
        Reflections reflections;
        try {
            reflections = createReflections(packagePath);
            classes = scanModulesInPackage(reflections);
        } catch (Exception e) {
            throw new ModuleDiscoveryException("Failed to discover modules from package: " + packagePath, e);
        }

        for (Class<? extends Module> clazz : classes) {
            ModuleInfo info = clazz.getAnnotation(ModuleInfo.class);
            ModuleDescriptor descriptor = ModuleDescriptor.from(clazz, info);
            ModuleRuntimeLibraryLoadResult libraryResult = loadRuntimeLibraries(descriptor);
            if (libraryResult.success()) {
                ModuleCandidate candidate = new ModuleCandidate(clazz, descriptor);
                discoveredModules.add(candidate);
            } else {
                issues.add(new RuntimeLibraryLoadFailedIssue(
                        descriptor,
                        clazz,
                        libraryResult,
                        "Failed to load runtime libraries: " + libraryResult.failureSummary()
                ));
                LOGGER.warn("Skipping module: {} ({}) - failed to load runtime libraries: {}",
                        descriptor.name(),
                        descriptor.id(),
                        libraryResult.failureSummary());
            }
        }
        return ModuleRegistrationStageResult.of(discoveredModules, new ModuleRegistrationIssues(issues));
    }

    private ModuleRuntimeLibraryLoadResult loadRuntimeLibraries(ModuleDescriptor descriptor) {
        if (runtimeLibraryLoader == null) {
            return ModuleRuntimeLibraryLoadResult.success(descriptor.id());
        }
        return runtimeLibraryLoader.load(descriptor);
    }

    private Reflections createReflections(String packagePath) {
        return new Reflections(
                packagePath,
                Scanners.SubTypes.filterResultsBy(c -> true)
        );
    }

    private List<Class<? extends Module>> scanModulesInPackage(Reflections reflections) {
        return reflections.getSubTypesOf(Module.class).stream()
                .filter(clazz -> clazz.isAnnotationPresent(ModuleInfo.class))
                .collect(Collectors.toList());
    }

}
