package io.fntlv.bluematrix.configtest.scan.config;

import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;

@ConfigRegister
@BlueConfig(category = "external")
public class ExternalScanConfig {

    @BlueConfig.Field(path = "name", defaultValue = "external")
    public String name;
}
