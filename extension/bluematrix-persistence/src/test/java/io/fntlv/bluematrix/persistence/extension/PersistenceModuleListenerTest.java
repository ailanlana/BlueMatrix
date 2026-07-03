package io.fntlv.bluematrix.persistence.extension;

import br.com.finalcraft.everydatabase.query.Query;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.persistence.core.data.BlueDataAccess;
import io.fntlv.bluematrix.persistence.core.data.BlueDataQueryAccess;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueKey;
import io.fntlv.bluematrix.persistence.core.storage.source.BlueInMemoryStorageSource;
import io.fntlv.bluematrix.persistence.core.storage.source.BlueStorageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceModuleListenerTest {
    @TempDir
    File tempDir;

    @Test
    void enableInitializesStorageAndRegistersBlueEntityDefinitions() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry();
        PersistenceModuleListener listener = new PersistenceModuleListener(tempDir, registry);
        ModuleCandidate candidate = candidate(PersistentModule.class);
        PersistentModule module = new PersistentModule();
        ModuleContext context = new ModuleContext(module, candidate);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        ModuleEnableEvent.Pre enable = new ModuleEnableEvent.Pre(context);
        listener.onEnablePre(enable);

        assertFalse(enable.hasError());
        assertTrue(registry.get(context.id()).storage().available());
        assertTrue(registry.get(context.id()).storage().registry().find(PersistentData.class).isPresent());
    }

    @Test
    void registerCreatesContextForConstructorInjection() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry();
        PersistenceModuleListener listener = new PersistenceModuleListener(tempDir, registry);
        ModuleCandidate candidate = candidate(PersistentModule.class);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        ModulePersistenceContext persistence = (ModulePersistenceContext) new PersistenceContextResolver(registry)
                .resolve(ModulePersistenceContext.class, ModuleInjectionContext.from(candidate));
        BlueDataAccess access = persistence.dataAccess();
        BlueDataQueryAccess queryAccess = persistence.queryAccess();

        assertSame(access, queryAccess);
        assertEquals(candidate.id(), persistence.moduleId());
        assertSame(registry.get(candidate.id()).state().dataAccess(), access);
        assertFalse(registry.get(candidate.id()).state().storage().available());
    }

    @Test
    void resolvedDataAccessCanReadAndWriteRegisteredData() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry();
        PersistenceModuleListener listener = new PersistenceModuleListener(tempDir, registry);
        ModuleCandidate candidate = candidate(PersistentModule.class);
        ModuleContext context = new ModuleContext(new PersistentModule(), candidate);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        ModulePersistenceContext persistence = (ModulePersistenceContext) new PersistenceContextResolver(registry)
                .resolve(ModulePersistenceContext.class, ModuleInjectionContext.from(candidate));
        BlueDataAccess access = persistence.dataAccess();
        PersistentData data = new PersistentData(PersistentData.ID, "loaded");

        access.save(data).join();
        Optional<PersistentData> found = access.get(PersistentData.class, PersistentData.ID).join();

        assertTrue(found.isPresent());
        assertEquals("loaded", found.get().name);
    }

    @Test
    void resolvedQueryAccessUsesSameAccessInstance() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry();
        PersistenceModuleListener listener = new PersistenceModuleListener(tempDir, registry);
        ModuleCandidate candidate = candidate(PersistentModule.class);
        ModuleContext context = new ModuleContext(new PersistentModule(), candidate);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        ModulePersistenceContext persistence = (ModulePersistenceContext) new PersistenceContextResolver(registry)
                .resolve(ModulePersistenceContext.class, ModuleInjectionContext.from(candidate));
        BlueDataAccess dataAccess = persistence.dataAccess();
        BlueDataQueryAccess queryAccess = persistence.queryAccess();

        dataAccess.save(new PersistentData(PersistentData.ID, "query")).join();
        List<PersistentData> rows = queryAccess.query(PersistentData.class, Query.all()).join();

        assertSame(dataAccess, queryAccess);
        assertEquals(1, rows.size());
        assertEquals("query", rows.get(0).name);
    }

    @Test
    void disableClosesAndUnregistersModuleState() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry();
        PersistenceModuleListener listener = new PersistenceModuleListener(tempDir, registry);
        ModuleCandidate candidate = candidate(PersistentModule.class);
        ModuleContext context = new ModuleContext(new PersistentModule(), candidate);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));

        listener.onDisablePost(new ModuleDisableEvent.Post(context));

        assertThrows(IllegalStateException.class, () -> registry.get(context.id()));
    }

    @Test
    void nonPersistentModuleCannotResolvePersistenceContext() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry();
        ModuleCandidate candidate = candidate(PlainModule.class);
        PersistenceContextResolver resolver = new PersistenceContextResolver(registry);

        assertThrows(IllegalStateException.class,
                () -> resolver.resolve(ModulePersistenceContext.class, ModuleInjectionContext.from(candidate)));
    }

    private static ModuleCandidate candidate(Class<? extends Module> moduleClass) {
        return new ModuleCandidate(moduleClass, moduleClass.getAnnotation(ModuleInfo.class));
    }

    @ModuleInfo(id = "persistent-module", name = "Persistent Module")
    private static final class PersistentModule implements Module, BlueStorageSourceProvider {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }

        @Override
        public BlueStorageSource getStorageSource() {
            return new BlueInMemoryStorageSource() {
            };
        }
    }

    @ModuleInfo(id = "plain-module", name = "Plain Module")
    private static final class PlainModule implements Module {
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

    @BlueEntity(collection = "persistent_data")
    public static final class PersistentData {
        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

        @BlueKey
        public UUID id;
        public String name;

        public PersistentData() {
        }

        private PersistentData(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
