package com.algomentor.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads real credentials (mail account, OpenAI key, etc.) from a plain text
 * file named {@code algomentor.properties} sitting next to pom.xml, in the
 * same directory the app is run from. This exists because wiring
 * environment variables through an IDE run configuration is easy to get
 * wrong silently (a typo'd variable name just reads as "not set", with no
 * error) - a file you can open and read is much harder to misconfigure.
 *
 * Environment variables still work too (checked as a fallback), for anyone
 * who prefers them or is deploying without a local file.
 *
 * IMPORTANT: algomentor.properties is listed in .gitignore - it holds real
 * secrets (a mail password, an API key) and must never be committed or
 * pushed to GitHub.
 */
public final class AppConfig {
    private static final Properties PROPS = new Properties();
    private static final String FILE_NAME = "algomentor.properties";

    static {
        Path path = Path.of(FILE_NAME);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                PROPS.load(in);
                System.out.println("[AppConfig] Loaded configuration from " + path.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("[AppConfig] Found " + FILE_NAME + " but could not read it: " + e.getMessage());
            }
        } else {
            System.out.println("[AppConfig] No " + FILE_NAME + " found at " + path.toAbsolutePath()
                    + " - falling back to environment variables only.");
        }
    }

    private AppConfig() {}

    /** Returns the config value, checking the properties file first, then the environment, then null. */
    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String fallback) {
        String fromFile = PROPS.getProperty(key);
        if (fromFile != null && !fromFile.isBlank()) return fromFile.trim();

        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();

        return fallback;
    }

    public static boolean isSet(String key) {
        String value = get(key);
        return value != null && !value.isBlank();
    }

    /**
     * Prints one clear line per required credential at startup, so it's
     * immediately obvious in the console which real features are active
     * and which are missing a key - instead of silently degrading and
     * leaving the person to guess why something "looks like a demo".
     */
    public static void printStartupReport() {
        System.out.println("==================== AlgoMentor configuration ====================");
        report("MAIL_USERNAME", "Real welcome/verification emails");
        report("MAIL_PASSWORD", "Real welcome/verification emails");
        report("GROQ_API_KEY", "AI Mentor Chat, Complexity Analysis, AI-generated problems (free, no card)");
        report("OPENAI_API_KEY", "Same AI features if AI_PROVIDER=openai instead");
        warnIfWrongProvider("OPENAI_API_KEY", "sk-ant-", "an Anthropic key");
        System.out.println("Edit algomentor.properties (project root) to set any of these for real.");
        System.out.println("====================================================================");
    }

    private static void report(String key, String feature) {
        String status = isSet(key) ? "SET" : "MISSING";
        System.out.printf("  %-20s [%s] -> %s%n", key, status, feature);
    }

    /** Catches the exact mistake of pasting one provider's key into the other's field. */
    private static void warnIfWrongProvider(String key, String wrongPrefix, String wrongProviderName) {
        String value = get(key);
        if (value != null && value.startsWith(wrongPrefix)) {
            System.out.printf("  !! %s looks like %s (starts with \"%s\") - "
                    + "double check you haven't swapped the two keys.%n", key, wrongProviderName, wrongPrefix);
        }
    }
}
