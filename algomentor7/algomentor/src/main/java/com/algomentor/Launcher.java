package com.algomentor;

/**
 * Separate entry point that does NOT extend javafx.application.Application.
 * Required for the shaded fat jar: `java -jar` refuses to run a jar whose
 * declared Main-Class extends Application unless JavaFX is on the module
 * path, even when the JavaFX classes are shaded into the same jar. Pointing
 * the manifest at this class instead sidesteps that check.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}