package com.company.framework.driver;

/**
 * Supported browser types, keyed off the lowercase string used in
 * {@code config.properties} (e.g. {@code browser=chrome}).
 *
 * <p>Adding a new browser (e.g. Safari) means adding one enum constant here
 * and one {@code case} branch in {@link DriverFactory} — nothing else in the
 * framework needs to change. This is the Open/Closed Principle in practice:
 * open for extension (new browsers), closed for modification (existing
 * browser logic and every caller of DriverFactory stay untouched).
 */
public enum BrowserType {
    CHROME,
    FIREFOX,
    EDGE;

    /**
     * Resolves a config string (e.g. "chrome", "FIREFOX", " edge ") to a
     * {@link BrowserType}, throwing a clear error for anything unsupported
     * rather than silently defaulting - a silent default here would mask a
     * typo in config.properties or a CI parameter.
     */
    public static BrowserType fromString(String browserName) {
        if (browserName == null || browserName.isBlank()) {
            throw new IllegalArgumentException("Browser name must not be null/blank");
        }
        try {
            return BrowserType.valueOf(browserName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported browser [" + browserName + "]. Supported values: chrome, firefox, edge", e);
        }
    }
}
