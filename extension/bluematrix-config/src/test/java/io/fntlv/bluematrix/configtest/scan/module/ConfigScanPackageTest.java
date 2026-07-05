package io.fntlv.bluematrix.configtest.scan.module;

import io.fntlv.bluematrix.config.extension.ConfigCapabilityTestSupport;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.configtest.scan.config.ExternalScanConfig;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigScanPackageTest {

    @TempDir
    File tempDir;

    @Test
    void configRegisterCanBeDiscoveredFromConfiguredScanPackage() {
        ScanPackageModule module = new ScanPackageModule();
        ModuleContext context = new ModuleContext(module, ScanPackageModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigContext configContext = ConfigCapabilityTestSupport.load(tempDir, context);

        ExternalScanConfig config = configContext.get(ExternalScanConfig.class);
        assertEquals("external", config.name);
    }

    @ModuleInfo(
            id = "scan-package-config",
            name = "Scan Package Config",
            scanPackages = "io.fntlv.bluematrix.configtest.scan.config"
    )
    private static class ScanPackageModule implements Module {
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
