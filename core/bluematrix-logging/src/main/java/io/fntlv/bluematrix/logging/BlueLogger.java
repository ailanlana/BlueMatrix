package io.fntlv.bluematrix.logging;

public interface BlueLogger {

    boolean isDebugEnabled();

    void setDebugEnabled(boolean enabled);

    void debug(String message);

    void debug(String format, Object... args);

    void info(String message);

    void info(String format, Object... args);

    void warn(String message);

    void warn(String format, Object... args);

    void error(String message);

    void error(String format, Object... args);

    void error(String message, Throwable throwable);

    default void infoBanner() {
        info(" ____   _               __  __         _          _       ");
        info("| __ ) | | _   _   ___ |  \\/  |  __ _ | |_  _ __ (_)__  __");
        info("|  _ \\ | || | | | / _ \\| |\\/| | / _` || __|| '__|| |\\ \\/ /");
        info("| |_) || || |_| ||  __/| |  | || (_| || |_ | |   | | >  < ");
        info("|____/ |_| \\__,_| \\___||_|  |_| \\__,_| \\__||_|   |_|/_/\\_\\ ");
    }
}
