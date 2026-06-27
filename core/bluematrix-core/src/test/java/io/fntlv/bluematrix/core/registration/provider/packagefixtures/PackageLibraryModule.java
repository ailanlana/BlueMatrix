package io.fntlv.bluematrix.core.registration.provider.packagefixtures;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;

@ModuleInfo(
        id = "package-library-module",
        name = "Package Library Module",
        libraries = "com.example:package-lib:1.0.0"
)
public final class PackageLibraryModule implements Module {
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
