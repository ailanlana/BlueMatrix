package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleCandidate {

    private final Class<? extends Module> moduleClass;
    private final ModuleInfo moduleInfo;
    private final Reflections reflections;

    public ModuleCandidate(Class<? extends Module> moduleClass, ModuleInfo moduleInfo){
        this(moduleClass, moduleInfo, createReflections(moduleClass, moduleInfo));
    }

    public ModuleCandidate(Class<? extends Module> moduleClass, ModuleInfo moduleInfo, Reflections reflections) {
        this.moduleClass = moduleClass;
        this.moduleInfo = moduleInfo;
        this.reflections = reflections;
    }

    private static Reflections createReflections(Class<? extends Module> moduleClass, ModuleInfo moduleInfo) {
        String[] scanPackages = resolveScanPackages(moduleClass, moduleInfo);
        FilterBuilder inputFilter = new FilterBuilder();
        ConfigurationBuilder configuration = new ConfigurationBuilder()
                .setScanners(
                        Scanners.SubTypes.filterResultsBy(c -> true),
                        Scanners.TypesAnnotated,
                        Scanners.MethodsAnnotated
                );
        for (String scanPackage : scanPackages) {
            inputFilter.includePackage(scanPackage);
            configuration.forPackage(scanPackage, moduleClass.getClassLoader());
        }
        configuration.filterInputsBy(inputFilter);
        return new Reflections(configuration);
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
}
