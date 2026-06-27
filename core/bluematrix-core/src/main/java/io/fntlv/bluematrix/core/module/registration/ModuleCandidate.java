package io.fntlv.bluematrix.core.module.registration;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleDescriptor;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import lombok.Getter;

@Getter
public class ModuleCandidate {

    private final Class<? extends Module> moduleClass;
    private final ModuleDescriptor descriptor;

    public ModuleCandidate(Class<? extends Module> moduleClass, ModuleInfo moduleInfo){
        this(moduleClass, ModuleDescriptor.from(moduleClass, moduleInfo));
    }

    public ModuleCandidate(Class<? extends Module> moduleClass, ModuleDescriptor descriptor) {
        if (moduleClass == null) {
            throw new IllegalArgumentException("moduleClass cannot be null");
        }
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        this.moduleClass = moduleClass;
        this.descriptor = descriptor;
    }

    public String id() {
        return descriptor.id();
    }

    public String name() {
        return descriptor.name();
    }

    public String description() {
        return descriptor.description();
    }

    public String[] dependencies() {
        return descriptor.dependencies();
    }

    public String[] softDependencies() {
        return descriptor.softDependencies();
    }

    public String[] libraries() {
        return descriptor.libraries();
    }

    public String[] repositories() {
        return descriptor.repositories();
    }

    public ModuleInfo.LoadOrder loadOrder() {
        return descriptor.loadOrder();
    }

    public boolean enableByDefault() {
        return descriptor.enableByDefault();
    }

    public String[] scanPackages() {
        return descriptor.scanPackages();
    }
}
