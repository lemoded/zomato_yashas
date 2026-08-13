package com.company.framework.utils;

import com.company.framework.constants.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Single point of access to every externalized configuration value.
 *
 * <p><b>Load order (lowest to highest precedence):</b>
 * <ol>
 *     <li>{@code config.properties} — base defaults, checked into git</li>
 *     <li>{@code config-<environment>.properties} — overlays base values per QA/UAT/PROD</li>
 *     <li>JVM system property (e.g. {@code -Dbrowser=firefox}) — always wins</li>
 * </ol>
 *
 * <p>This class is the <b>only</b> class in the framework permitted to touch
 * {@code config*.properties} directly (Single Responsibility Principle) — every
 * other class (DriverFactory, BaseTest, ScreenshotUtils, ...) must go through
 * {@link #getProperty(String)} or one of the typed convenience getters below.
 *
 * <p>Loaded once into a static {@link Properties} object at class-init time.
 * Reads afterwards are simple in-memory map lookups, so this is safe and cheap
 * to call from parallel threads without extra synchronization — {@link Properties}
 * extends {@link java.util.Hashtable}, whose individual get/put operations are
 * already thread-safe.
 */
public final class PropertyUtils {

    private static final Logger log = LogManager.getLogger(PropertyUtils.class);
    private static final Properties PROPERTIES = new Properties();

    static {
        loadBaseProperties();
        loadEnvironmentOverrides();
    }

    private PropertyUtils() {
        throw new UnsupportedOperationException("PropertyUtils is a static utility class and cannot be instantiated");
    }

    // ===================== Loading =====================

    private static void loadBaseProperties() {
        try (InputStream input = new FileInputStream(FrameworkConstants.CONFIG_FILE_PATH)) {
            PROPERTIES.load(input);
            log.info("Loaded base configuration from [{}]", FrameworkConstants.CONFIG_FILE_PATH);
        } catch (IOException e) {
            // Deliberately fatal: without base config, no test can meaningfully run.
            // We fail fast here rather than letting every downstream getter NPE mysteriously.
            log.error("Unable to load base config.properties from [{}]", FrameworkConstants.CONFIG_FILE_PATH, e);
            throw new IllegalStateException(
                    "Framework configuration file not found or unreadable: " + FrameworkConstants.CONFIG_FILE_PATH, e);
        }
    }

    private static void loadEnvironmentOverrides() {
        // environment itself may already be overridden via -Denvironment=uat;
        // check system property directly since PROPERTIES isn't fully populated yet at this point.
        String environment = System.getProperty("environment", PROPERTIES.getProperty("environment", "qa"));
        String envFilePath = FrameworkConstants.getEnvConfigFilePath(environment);

        try (InputStream input = new FileInputStream(envFilePath)) {
            Properties overrides = new Properties();
            overrides.load(input);
            PROPERTIES.putAll(overrides);
            log.info("Applied environment overrides for [{}] from [{}]", environment, envFilePath);
        } catch (IOException e) {
            // Non-fatal: an environment-specific override file is optional.
            // Base config.properties values remain in effect.
            log.warn("No environment-specific config found for [{}] at [{}] - continuing with base config only",
                    environment, envFilePath);
        }
    }

    // ===================== Generic access =====================

    /**
     * Resolves a config key with system-property override precedence.
     * A JVM system property (e.g. {@code -Dbrowser=firefox}) always wins over
     * whatever is in the properties files.
     *
     * @throws IllegalArgumentException if the key exists in neither system properties nor the config files
     */
    public static String getProperty(String key) {
        String systemOverride = System.getProperty(key);
        if (systemOverride != null && !systemOverride.isBlank()) {
            log.debug("Property [{}] resolved from system property override: [{}]", key, systemOverride);
            return systemOverride;
        }

        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            log.error("Missing required configuration property: [{}]", key);
            throw new IllegalArgumentException(
                    "Configuration property [" + key + "] was not found in config.properties, "
                            + "environment overrides, or system properties.");
        }
        return value;
    }

    /** Same as {@link #getProperty(String)} but returns {@code defaultValue} instead of throwing. */
    public static String getProperty(String key, String defaultValue) {
        try {
            return getProperty(key);
        } catch (IllegalArgumentException e) {
            log.debug("Property [{}] not found, using default [{}]", key, defaultValue);
            return defaultValue;
        }
    }

    // ===================== Typed convenience getters =====================

    public static String getBrowser() {
        return getProperty("browser", "chrome").trim().toLowerCase();
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(getProperty("headless", "false").trim());
    }

    public static String getUrl() {
        return getProperty("url");
    }

    public static String getEnvironment() {
        return getProperty("environment", "qa").trim().toLowerCase();
    }

    public static int getImplicitWait() {
        return parseIntSafely("implicitWait", FrameworkConstants.DEFAULT_IMPLICIT_WAIT);
    }

    public static int getExplicitWait() {
        return parseIntSafely("explicitWait", FrameworkConstants.DEFAULT_EXPLICIT_WAIT);
    }

    public static int getPageLoadTimeout() {
        return parseIntSafely("pageLoadTimeout", FrameworkConstants.DEFAULT_PAGE_LOAD_TIMEOUT);
    }

    public static int getScriptTimeout() {
        return parseIntSafely("scriptTimeout", FrameworkConstants.DEFAULT_SCRIPT_TIMEOUT);
    }

    public static boolean isScreenshotOnFailureEnabled() {
        return Boolean.parseBoolean(getProperty("screenshotOnFailure", "true").trim());
    }

    private static int parseIntSafely(String key, int fallback) {
        String raw = getProperty(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value [{}] for property [{}], falling back to [{}]", raw, key, fallback);
            return fallback;
        }
    }
}
