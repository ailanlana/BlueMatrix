package io.fntlv.bluematrix.logging.backend;

public interface BlueLogBackendProvider {

    BlueLogBackend getBackend(String name);
}
