package com.company.framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.company.framework.constants.FrameworkConstants;
import com.company.framework.utils.PropertyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Owns the single {@link ExtentReports} instance for the entire test run.
 *
 * <p>Exactly one {@code ExtentReports} object must exist per execution - it
 * owns the HTML report file handle and system-info block. Creating more than
 * one (e.g. accidentally instantiating it per test thread) would either
 * overwrite the report on every flush or produce multiple competing report
 * files, depending on timing. This class exists solely to guarantee "exactly
 * once" initialization (Single Responsibility Principle: this class's only
 * job is lifecycle-managing the report instance, nothing else).
 *
 * <p><b>Thread safety:</b> {@link #getInstance()} uses the double-checked
 * locking pattern with a {@code volatile} field, so concurrent TestNG threads
 * calling it for the first time (e.g. in {@code @BeforeSuite} racing with
 * early {@code @BeforeMethod} calls) still only trigger one real
 * initialization. Once created, the underlying {@code ExtentReports.createTest()}
 * method is itself internally synchronized by the ExtentReports library, so
 * multiple threads safely create their own {@code ExtentTest} nodes against
 * this same shared instance - see {@link ExtentReport} for the per-thread
 * {@code ExtentTest} handling.
 */
public final class ExtentManager {

    private static final Logger log = LogManager.getLogger(ExtentManager.class);
    private static volatile ExtentReports extentReports;
    private static final Object INIT_LOCK = new Object();

    private ExtentManager() {
        throw new UnsupportedOperationException("ExtentManager is a static utility class and cannot be instantiated");
    }

    /**
     * Returns the shared {@link ExtentReports} instance, creating it on first
     * call. Safe to call from any thread at any point in the suite lifecycle.
     */
    public static ExtentReports getInstance() {
        ExtentReports result = extentReports;
        if (result == null) {
            synchronized (INIT_LOCK) {
                result = extentReports;
                if (result == null) {
                    extentReports = result = createInstance();
                }
            }
        }
        return result;
    }

    private static ExtentReports createInstance() {
        ensureReportDirectoryExists();

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(FrameworkConstants.EXTENT_REPORT_FILE_PATH);
        sparkReporter.config().setDocumentTitle("Automation Test Report");
        sparkReporter.config().setReportName("Regression Suite Execution Report");
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        ExtentReports extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        // System info block - visible at the top of the HTML report, invaluable
        // when triaging "why did this pass on my machine but fail in Jenkins".
        extent.setSystemInfo("Environment", PropertyUtils.getEnvironment());
        extent.setSystemInfo("Browser", PropertyUtils.getBrowser());
        extent.setSystemInfo("Headless", String.valueOf(PropertyUtils.isHeadless()));
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("Executed By", System.getProperty("user.name"));

        log.info("ExtentReports initialized. Report will be written to [{}]", FrameworkConstants.EXTENT_REPORT_FILE_PATH);
        return extent;
    }

    private static void ensureReportDirectoryExists() {
        File reportDir = new File(FrameworkConstants.REPORT_PATH);
        if (!reportDir.exists() && reportDir.mkdirs()) {
            log.debug("Created report directory: [{}]", reportDir.getAbsolutePath());
        }
    }

    /**
     * Writes all buffered test results to the HTML report file.
     * Must be called exactly once, at the very end of the suite
     * ({@code @AfterSuite} / {@code TestListener.onFinish()}) - calling it
     * mid-suite is harmless but unnecessary; not calling it at all means the
     * report file is never written.
     */
    public static synchronized void flush() {
        if (extentReports != null) {
            extentReports.flush();
            log.info("ExtentReports flushed to disk: [{}]", FrameworkConstants.EXTENT_REPORT_FILE_PATH);
        } else {
            log.warn("flush() called but ExtentReports was never initialized - no report to write");
        }
    }
}
