package com.robotmonitor;

import com.robotmonitor.ui.MonitorApp;
import javafx.application.Application;

/** Separate launcher allows JavaFX native libraries to load from the packaged JAR. */
public final class Launcher {
    private Launcher() {}
    public static void main(String[] args) {
        if (args.length > 0 && "--console".equals(args[0])) ConsoleApp.main(args);
        else Application.launch(MonitorApp.class, args);
    }
}
