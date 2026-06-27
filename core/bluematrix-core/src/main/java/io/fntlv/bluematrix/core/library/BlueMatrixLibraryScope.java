package io.fntlv.bluematrix.core.library;

import java.io.File;

public enum BlueMatrixLibraryScope {
    CORE("BlueMatrixCore", "core"),
    EXTENSION("BlueMatrixExtensions", "extensions"),
    MODULE("BlueMatrixModules", "modules"),
    APP("BlueMatrixApp", "app");

    private final String managerName;
    private final String folderName;

    BlueMatrixLibraryScope(String managerName, String folderName) {
        this.managerName = managerName;
        this.folderName = folderName;
    }

    public String managerName(String qualifier) {
        if (qualifier == null || qualifier.trim().isEmpty()) {
            return managerName;
        }
        return managerName + "_" + qualifier;
    }

    public File rootFolder(File dataFolder) {
        File libsFolder = new File(dataFolder, "libs");
        if (this == EXTENSION || this == MODULE) {
            return new File(libsFolder, folderName);
        }
        return libsFolder;
    }

    public String libsFolderName(String qualifier) {
        if (this == EXTENSION || this == MODULE) {
            return qualifier;
        }
        return folderName;
    }
}
