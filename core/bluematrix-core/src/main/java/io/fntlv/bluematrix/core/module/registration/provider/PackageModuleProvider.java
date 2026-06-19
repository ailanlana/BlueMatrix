package io.fntlv.bluematrix.core.module.registration.provider;

import io.fntlv.bluematrix.core.module.registration.exception.ModuleDiscoveryException;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PackageModuleProvider implements ModuleProvider {
    private final String packagePath;

    public PackageModuleProvider(String packagePath){
        this.packagePath = packagePath;
    }

    @Override
    public List<ModuleCandidate> discoverModules() {
        List<ModuleCandidate> discoveredModules = new ArrayList<>();
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
            ModuleCandidate candidate = new ModuleCandidate(clazz, info);
            discoveredModules.add(candidate);
        }
        return new ArrayList<>(discoveredModules);
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
