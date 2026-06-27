package io.fntlv.bluematrix.core.registration.provider.packagefixtures;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;

@ModuleInfo(id = "package-plain-module", name = "Package Plain Module")
public final class PackagePlainModule implements Module {
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
