package com.company.framework.constants;

import java.io.File;

/**
 * Central repository of every path and default value used across the framework.
 *
 * <p>Rationale: paths (config file, screenshots, reports, logs, test data) were
 * previously scattered across DriverFactory, ScreenshotUtils, ExtentManager, etc.
 * Keeping them here means:
 * <ul>
 *     <li>a single change point if the project layout ever moves</li>
 *     <li>no risk of two classes silently disagreeing on where a file lives</li>
 *     <li>every path is OS-independent (built with {@link File#separator})</li>
 * </ul>
 *
 * <p>This class is a pure constants holder: no state, no behaviour.
 * The private constructor prevents instantiation and satisfies the
 * "utility class" convention enforced by static analysis tools like SonarQube.
 */
public final class FrameworkConstants {

    private FrameworkConstants() {
        // Prevent instantiation - this is a static constants holder only.
        throw new UnsupportedOperationException("FrameworkConstants is a constants class and cannot be instantiated");
    }

    // ===================== Project root =====================
    /** Absolute path to the project root (wherever Maven/IDE is executing from). */
    public static final String PROJECT_PATH = System.getProperty("user.dir");

    // ===================== Configuration =====================
    public static final String CONFIG_FILE_PATH =
            PROJECT_PATH + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources" + File.separator + "config.properties";

    /**
     * Environment-specific override file, e.g. config-qa.properties / config-uat.properties.
     * Resolved dynamically by PropertyUtils based on the active `environment` value,
     * so it is intentionally a method rather than a fixed constant.
     */
    public static String getEnvConfigFilePath(String environment) {
        return PROJECT_PATH + File.separator + "src" + File.separator + "main"
                + File.separator + "resources" + File.separator + "config-" + environment + ".properties";
    }

    // ===================== Test data =====================
    public static final String TEST_DATA_PATH =
            PROJECT_PATH + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources" + File.separator + "testdata" + File.separator;

    public static final String EXCEL_FILE_PATH = TEST_DATA_PATH + "TestData.xlsx";

    // ===================== Screenshots =====================
    public static final String SCREENSHOT_PATH = PROJECT_PATH + File.separator + "screenshots" + File.separator;

    // ===================== Reports =====================
    public static final String REPORT_PATH = PROJECT_PATH + File.separator + "reports" + File.separator;
    public static final String EXTENT_REPORT_FILE_PATH = REPORT_PATH + "ExtentReport.html";
    public static final String EXTENT_CONFIG_XML_PATH =
            PROJECT_PATH + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources" + File.separator + "extent-config.xml";

    // ===================== Logs =====================
    public static final String LOG_PATH = PROJECT_PATH + File.separator + "logs" + File.separator;

    // ===================== Default timeouts (seconds) =====================
    // These are FALLBACK values only, used if config.properties is missing a key.
    // The framework should always prefer the value read via PropertyUtils.
    public static final int DEFAULT_IMPLICIT_WAIT = 15;
    public static final int DEFAULT_EXPLICIT_WAIT = 15;
    public static final int DEFAULT_PAGE_LOAD_TIMEOUT = 30;
    public static final int DEFAULT_SCRIPT_TIMEOUT = 30;
    public static final int FLUENT_WAIT_POLLING_MILLIS = 500;

    // ===================== Misc =====================
    public static final String DATE_FORMAT_PATTERN = "yyyyMMdd_HHmmss";
}
