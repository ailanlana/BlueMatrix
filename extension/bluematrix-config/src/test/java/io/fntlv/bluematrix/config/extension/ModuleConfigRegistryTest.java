package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleConfigRegistryTest {

    @TempDir
    File tempDir;

    @Test
    void modulePathUsesModulesDirectory() {
        ModuleConfigRegistry registry = registry();

        assertEquals(new File(tempDir, "modules/example"), registry.getModulePath("example"));
    }

    @Test
    void registryUsesExplicitYamlFormat() {
        ModuleConfigRegistry registry = registry();

        assertEquals("yaml", registry.getFileFormat().name());
    }

    @Test
    void opensNamedYamlConfigFileInModulesDirectory() {
        ModuleConfigRegistry registry = registry();

        assertEquals(new File(tempDir, "modules/example/database.yml"),
                registry.openFile("example", "database").getFile());
        assertEquals(new File(tempDir, "modules/example/database.yml"),
                registry.openFile("example", "database.yml").getFile());
    }

    @Test
    void rejectsUnsafeConfigFileNames() {
        ModuleConfigRegistry registry = registry();

        assertThrows(IllegalArgumentException.class, () -> registry.openFile("example", " "));
        assertThrows(IllegalArgumentException.class, () -> registry.openFile("example", "../database"));
        assertThrows(IllegalArgumentException.class, () -> registry.openFile("example", "folder/database"));
        assertThrows(IllegalArgumentException.class, () -> registry.openFile("example", "folder\\database"));
    }

    @Test
    void registersModuleConfigContextByCandidate() {
        ModuleConfigRegistry registry = registry();
        ModuleCandidate candidate = candidate();

        ModuleConfigContext configContext = registry.registerContext(candidate);

        assertEquals("example", configContext.moduleId());
        assertSame(configContext, registry.getContext(candidate));
        assertThrows(IllegalStateException.class, configContext::module);
    }

    @Test
    void reusesContextForSameModuleId() {
        ModuleConfigRegistry registry = registry();
        ModuleCandidate candidate = candidate();

        ModuleConfigContext first = registry.registerContext(candidate);
        ModuleConfigContext second = registry.registerContext(candidate);

        assertSame(first, second);
    }

    @Test
    void bindContextConnectsModuleAndState() {
        ModuleConfigRegistry registry = registry();
        ExampleModule module = new ExampleModule();
        ModuleContext context = new ModuleContext(module, ExampleModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigContext configContext = registry.registerContext(candidate());
        ModuleConfigState state = new ModuleConfigState(
                module,
                context.getInfo().id(),
                registry.openFile(context.getInfo().id())
        );

        registry.bindContext(context, state);

        assertSame(module, configContext.module());
        assertSame(configContext, registry.getContext(context));
        assertSame(state, registry.getState(context));
        assertEquals(new File(tempDir, "modules/example/config.yml"), state.file().getFile());
    }

    @Test
    void singleFileStateUsesRegistryFileNameNormalization() {
        ModuleConfigRegistry registry = registry();
        ExampleModule module = new ExampleModule();
        ModuleConfigState state = new ModuleConfigState(module, "example", registry.openFile("example"));

        assertSame(state.file(), state.file("config"));
        assertSame(state.file(), state.file("config.yml"));
        assertThrows(IllegalStateException.class, () -> state.file("database"));
        assertThrows(IllegalArgumentException.class, () -> state.file("../config"));
    }

    @Test
    void rejectsUnregisteredContextLookup() {
        ModuleConfigRegistry registry = registry();

        IllegalStateException candidateException = assertThrows(IllegalStateException.class,
                () -> registry.getContext(candidate()));
        assertExceptionMessageContains(candidateException, "ModuleConfigContext should be registered for every module");
        assertExceptionMessageContains(candidateException, "unexpected config extension lifecycle state");
        assertExceptionMessageContains(candidateException, "example");
        assertExceptionMessageContains(candidateException, ExampleModule.class.getName());

        ExampleModule module = new ExampleModule();
        ModuleContext context = new ModuleContext(module, ExampleModule.class.getAnnotation(ModuleInfo.class));
        IllegalStateException contextException = assertThrows(IllegalStateException.class,
                () -> registry.getContext(context));
        assertExceptionMessageContains(contextException, "ModuleConfigContext should be registered for every module");
        assertExceptionMessageContains(contextException, "unexpected config extension lifecycle state");
        assertExceptionMessageContains(contextException, "example");
        assertExceptionMessageContains(contextException, ExampleModule.class.getName());
    }

    @Test
    void contextRejectsSecondStateBinding() {
        ModuleConfigRegistry registry = registry();
        ExampleModule module = new ExampleModule();
        ModuleContext context = new ModuleContext(module, ExampleModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigState first = new ModuleConfigState(module, context.getInfo().id(), registry.openFile(context.getInfo().id()));
        ModuleConfigState second = new ModuleConfigState(module, context.getInfo().id(), registry.openFile(context.getInfo().id()));

        registry.bindContext(context, first);

        assertThrows(IllegalStateException.class, () -> registry.bindContext(context, second));
    }

    private ModuleConfigRegistry registry() {
        return new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());
    }

    private ModuleCandidate candidate() {
        return new ModuleCandidate(ExampleModule.class, ExampleModule.class.getAnnotation(ModuleInfo.class));
    }

    private static void assertExceptionMessageContains(Throwable throwable, String expected) {
        if (throwable.getMessage() == null || !throwable.getMessage().contains(expected)) {
            throw new AssertionError("Expected exception message to contain: " + expected);
        }
    }

    @ModuleInfo(id = "example", name = "Example")
    private static class ExampleModule implements Module {
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
}
