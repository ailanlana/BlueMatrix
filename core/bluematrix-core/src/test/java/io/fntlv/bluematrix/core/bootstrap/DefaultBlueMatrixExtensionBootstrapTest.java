package io.fntlv.bluematrix.core.bootstrap;

import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.library.BlueMatrixLibraryLoader;
import io.fntlv.bluematrix.core.module.instance.InjectContext;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultBlueMatrixExtensionBootstrapTest {
    @TempDir
    File tempDir;

    @Test
    void exposesExtensionViewOverBootstrapPlan() {
        BlueMatrixBootstrapPlan plan = new BlueMatrixBootstrapPlan(
                tempDir,
                getClass().getClassLoader(),
                new BlueMatrixLibraryLoader(tempDir, getClass().getClassLoader())
        );
        BlueMatrixExtensionBootstrap bootstrap = new DefaultBlueMatrixExtensionBootstrap(plan);
        Object listener = new Object();
        ModuleParameterResolver resolver = new TestResolver();

        assertSame(tempDir, bootstrap.dataFolder());
        assertSame(bootstrap, bootstrap.eventListener(listener));
        assertSame(bootstrap, bootstrap.parameterResolver(resolver));

        assertEquals(1, plan.eventListeners().size());
        assertSame(listener, plan.eventListeners().get(0));
        assertEquals(1, plan.parameterResolvers().size());
        assertSame(resolver, plan.parameterResolvers().get(0));
    }

    private static final class TestResolver implements ModuleParameterResolver {
        @Override
        public boolean supports(Class<?> parameterType, InjectContext context) {
            return false;
        }

        @Override
        public Object resolve(Class<?> parameterType, InjectContext context) {
            return null;
        }
    }
}
