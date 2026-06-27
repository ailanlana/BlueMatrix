package io.fntlv.bluematrix.core.module;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public final class ModuleReflectionsFactory {
    private ModuleReflectionsFactory() {
    }

    public static Reflections create(Class<? extends Module> moduleClass, ModuleDescriptor descriptor) {
        if (moduleClass == null) {
            throw new IllegalArgumentException("moduleClass cannot be null");
        }
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        FilterBuilder inputFilter = new FilterBuilder();
        ConfigurationBuilder configuration = new ConfigurationBuilder()
                .setScanners(
                        Scanners.SubTypes.filterResultsBy(c -> true),
                        Scanners.TypesAnnotated,
                        Scanners.MethodsAnnotated
                );
        for (String scanPackage : descriptor.scanPackages()) {
            inputFilter.includePackage(scanPackage);
            configuration.forPackage(scanPackage, moduleClass.getClassLoader());
        }
        configuration.filterInputsBy(inputFilter);
        return new Reflections(configuration);
    }
}
