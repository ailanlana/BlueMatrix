package io.fntlv.bluematrix.core.module.storage;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModuleStore {
    private final List<ModuleContext> modules = new CopyOnWriteArrayList<>();

    public void add(ModuleContext moduleContext) {
        modules.add(moduleContext);
    }

    public List<ModuleContext> all() {
        return Collections.unmodifiableList(new ArrayList<>(modules));
    }

    public <T extends Module> Optional<ModuleContext> findByClass(Class<T> clazz) {
        return modules.stream()
                .filter(moduleContext -> clazz.isInstance(moduleContext.getInstance()))
                .findFirst();
    }

    public Optional<ModuleContext> findById(String moduleID) {
        return modules.stream()
                .filter(moduleContext -> moduleContext.id().equals(moduleID))
                .findAny();
    }

    public Optional<ModuleContext> findByInstance(Module module) {
        return modules.stream()
                .filter(moduleContext -> moduleContext.getInstance().equals(module))
                .findFirst();
    }

    public int size() {
        return modules.size();
    }
}
