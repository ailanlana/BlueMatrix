package io.fntlv.bluematrix.persistence.extension;

import br.com.finalcraft.everydatabase.query.Query;
import br.com.finalcraft.everydatabase.changefeed.ChangeEvent;
import br.com.finalcraft.everydatabase.changefeed.ChangeFeedSupport;
import br.com.finalcraft.everydatabase.changefeed.ChangeListener;
import br.com.finalcraft.everydatabase.changefeed.ChangeSubscription;
import br.com.finalcraft.everydatabase.manager.sync.CacheSyncTransport;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityContextResolver;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityListener;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityRegistry;
import io.fntlv.bluematrix.core.module.instance.ModuleInjectionContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.persistence.core.cache.BlueCache;
import io.fntlv.bluematrix.persistence.core.cache.BlueCachePolicy;
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
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceCapabilityTest {
    @TempDir
    File tempDir;

    @Test
    void enableInitializesStorageAndRegistersBlueEntityDefinitions() {
        ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability = capability();
        ModuleCapabilityListener listener = listener(capability);
        ModuleCandidate candidate = candidate(PersistentModule.class);
        PersistentModule module = new PersistentModule();
        ModuleContext context = new ModuleContext(module, candidate);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        ModuleEnableEvent.Pre enable = new ModuleEnableEvent.Pre(context);
        listener.onEnablePre(enable);

        assertFalse(enable.hasError());
        assertTrue(capability.getState(context.id()).storage().available());
        assertTrue(capability.getState(context.id()).storage().registry().find(PersistentData.class).isPresent());
    }

    @Test
    void registerCreatesContextForConstructorInjection() {
        ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability = capability();
        ModuleCapabilityRegistry registry = registry(capability);
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleCandidate candidate = candidate(PersistentModule.class);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        ModulePersistenceContext persistence = (ModulePersistenceContext) new ModuleCapabilityContextResolver(registry)
                .resolve(ModulePersistenceContext.class, ModuleInjectionContext.from(candidate));
        BlueDataAccess access = persistence.dataAccess();
        BlueDataQueryAccess queryAccess = persistence.queryAccess();

        assertSame(access, queryAccess);
        assertEquals(candidate.id(), persistence.moduleId());
        assertSame(capability.getState(candidate.id()).dataAccess(), access);
        assertFalse(capability.getState(candidate.id()).storage().available());
    }

    @Test
    void resolvedDataAccessCanReadAndWriteRegisteredData() {
        ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability = capability();
        ModuleCapabilityRegistry registry = registry(capability);
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleCandidate candidate = candidate(PersistentModule.class);
        ModuleContext context = new ModuleContext(new PersistentModule(), candidate);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        ModulePersistenceContext persistence = (ModulePersistenceContext) new ModuleCapabilityContextResolver(registry)
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
        ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability = capability();
        ModuleCapabilityRegistry registry = registry(capability);
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleCandidate candidate = candidate(PersistentModule.class);
        ModuleContext context = new ModuleContext(new PersistentModule(), candidate);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        ModulePersistenceContext persistence = (ModulePersistenceContext) new ModuleCapabilityContextResolver(registry)
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
    void persistentModuleCanProvideCacheSyncTransport() {
        ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability = capability();
        ModuleCapabilityListener listener = listener(capability);
        ModuleCandidate candidate = candidate(SyncedPersistentModule.class);
        RecordingTransport transport = new RecordingTransport();
        ModuleContext context = new ModuleContext(new SyncedPersistentModule(transport), candidate);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        capability.getState(context.id()).dataAccess()
                .save(new SyncedPersistentData(SyncedPersistentData.ID, "sync"))
                .join();

        assertEquals(1, transport.published.size());
        assertEquals("synced_persistent_data", transport.published.get(0).collection());
    }

    @Test
    void disableClosesAndUnregistersModuleState() {
        ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability = capability();
        ModuleCapabilityListener listener = listener(capability);
        ModuleCandidate candidate = candidate(PersistentModule.class);
        ModuleContext context = new ModuleContext(new PersistentModule(), candidate);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onEnablePre(new ModuleEnableEvent.Pre(context));

        listener.onDisablePost(new ModuleDisableEvent.Post(context));

        assertFalse(capability.contains(context.id()));
    }

    @Test
    void nonPersistentModuleCannotResolvePersistenceContext() {
        ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability = capability();
        ModuleCapabilityRegistry registry = registry(capability);
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleCandidate candidate = candidate(PlainModule.class);
        ModuleCapabilityContextResolver resolver = new ModuleCapabilityContextResolver(registry);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        assertThrows(IllegalStateException.class,
                () -> resolver.resolve(ModulePersistenceContext.class, ModuleInjectionContext.from(candidate)));
    }

    private ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability() {
        ModulePersistenceInitializer initializer = new ModulePersistenceInitializer(tempDir);
        return ModuleCapability.<ModulePersistenceContext, ModulePersistenceState>builder("persistence")
                .contextType(ModulePersistenceContext.class)
                .enabledWhen(candidate -> BlueStorageSourceProvider.class.isAssignableFrom(candidate.getModuleClass()))
                .stateFactory(moduleId -> new ModulePersistenceState())
                .contextFactory(ModulePersistenceContext::new)
                .onEnablePre((binding, event) -> initializer.initialize(
                        event.getContext(),
                        binding.state().storage(),
                        binding.state().cacheSyncCoordinator()
                ))
                .onDisablePost((binding, event) -> close(binding.state()))
                .onDisableFailed((binding, event) -> close(binding.state()))
                .build();
    }

    private static void close(ModulePersistenceState state) {
        state.dataAccess().flushAllDirty().join();
        state.cacheSyncCoordinator().close();
        state.storage().close();
    }

    private static ModuleCapabilityListener listener(
            ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability) {
        return new ModuleCapabilityListener(registry(capability));
    }

    private static ModuleCapabilityRegistry registry(
            ModuleCapability<ModulePersistenceContext, ModulePersistenceState> capability) {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        registry.register(capability);
        return registry;
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

    @ModuleInfo(id = "synced-persistent-module", name = "Synced Persistent Module")
    private static final class SyncedPersistentModule implements Module,
            BlueStorageSourceProvider,
            BlueCacheSyncTransportProvider {
        private final CacheSyncTransport transport;

        private SyncedPersistentModule(CacheSyncTransport transport) {
            this.transport = transport;
        }

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

        @Override
        public CacheSyncTransport getCacheSyncTransport() {
            return transport;
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

    @BlueEntity(collection = "synced_persistent_data")
    @BlueCache(policy = BlueCachePolicy.ALWAYS)
    public static final class SyncedPersistentData {
        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000041");

        @BlueKey
        public UUID id;
        public String name;

        public SyncedPersistentData() {
        }

        private SyncedPersistentData(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class RecordingTransport implements CacheSyncTransport {
        private final List<ChangeEvent> published = new ArrayList<>();
        private final ChangeFeedSupport feed = new ChangeFeedSupport();

        @Override
        public String originId() {
            return "recording";
        }

        @Override
        public void publish(ChangeEvent event) {
            published.add(event);
        }

        @Override
        public ChangeSubscription subscribe(ChangeListener listener) {
            return feed.subscribe(listener);
        }

        @Override
        public void onConnectionStateChanged(Consumer<Boolean> listener) {
            if (listener != null) {
                listener.accept(Boolean.TRUE);
            }
        }

        @Override
        public void close() {
            feed.closeAll();
        }
    }
}
