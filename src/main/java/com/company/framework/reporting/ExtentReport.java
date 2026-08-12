package com.company.framework.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe holder for the current thread's {@link ExtentTest} node.
 *
 * <p>Just like {@link com.company.framework.driver.DriverManager} isolates
 * WebDriver per thread, this class isolates the Extent report's "current
 * test" pointer per thread. Without this, {@code ExtentTest} log calls from
 * two parallel TestNG threads would interleave onto whichever test node was
 * created most recently - producing a report where Test A's steps show up
 * under Test B's entry. {@link ThreadLocal} guarantees each thread only ever
 * logs to its own node.
 *
 * <p>Also centralizes the "log to Extent AND log to Log4j2 in one call"
 * convenience methods ({@link #pass}, {@link #fail}, {@link #skip},
 * {@link #info}) so {@code TestListener} and page objects don't need to
 * remember to do both separately every time.
 */
public final class ExtentReport {

    private static final Logger log = LogManager.getLogger(ExtentReport.class);
    private static final ThreadLocal<ExtentTest> TEST_THREAD_LOCAL = new ThreadLocal<>();

    private ExtentReport() {
        throw new UnsupportedOperationException("ExtentReport is a static utility class and cannot be instantiated");
    }

    /**
     * Creates a new Extent test node for the calling thread and binds it as
     * that thread's current test. Called once per test method, typically from
     * {@code TestListener.onTestStart()}.
     */
    public static void createTest(String testName, String description) {
        ExtentTest test = ExtentManager.getInstance().createTest(testName, description);
        TEST_THREAD_LOCAL.set(test);
        log.debug("Extent test node created: [{}] on thread [{}]", testName, Thread.currentThread().getId());
    }

    /**
     * @return the {@link ExtentTest} bound to the calling thread
     * @throws IllegalStateException if {@link #createTest} was never called on this thread -
     *                                fails loudly rather than silently no-op'ing, so a missing
     *                                {@code onTestStart()} call is caught immediately in dev,
     *                                not discovered as a blank report in CI.
     */
    public static ExtentTest getTest() {
        ExtentTest test = TEST_THREAD_LOCAL.get();
        if (test == null) {
            throw new IllegalStateException(
                    "No ExtentTest bound to thread [" + Thread.currentThread().getId() + "]. "
                            + "Ensure ExtentReport.createTest() runs (via TestListener.onTestStart()) "
                            + "before any pass/fail/info logging.");
        }
        return test;
    }

    /** Clears the ThreadLocal reference. Call in {@code onTestSuccess}/{@code onTestFailure}/{@code onTestSkipped} cleanup. */
    public static void removeTest() {
        TEST_THREAD_LOCAL.remove();
        log.debug("ExtentTest reference cleared for thread [{}]", Thread.currentThread().getId());
    }

    // ===================== Convenience logging (Extent + Log4j2 together) =====================

    public static void pass(String message) {
        getTest().log(Status.PASS, message);
        log.info(message);
    }

    public static void fail(String message) {
        getTest().log(Status.FAIL, message);
        log.error(message);
    }

    public static void skip(String message) {
        getTest().log(Status.SKIP, message);
        log.warn(message);
    }

    public static void info(String message) {
        getTest().log(Status.INFO, message);
        log.info(message);
    }

    /**
     * Attaches a screenshot (by file path) to the current thread's test node.
     * Silently degrades (logs a warning, doesn't throw) if attachment fails -
     * a broken screenshot attachment must never mask the actual test result.
     */
    public static void attachScreenshot(String screenshotPath) {
        if (screenshotPath == null || screenshotPath.isBlank()) {
            log.debug("attachScreenshot() called with no path - skipping attachment");
            return;
        }
        try {
            getTest().addScreenCaptureFromPath(screenshotPath);
        } catch (Exception e) {
            // Deliberately broad: ExtentReports 5.1.2's addScreenCaptureFromPath(String) does not
            // declare a checked exception (older versions/docs suggest IOException, which is why
            // catching IOException specifically fails to compile here - "exception is never thrown").
            // Catching Exception keeps this resilient across library versions without recompiling,
            // while still logging meaningfully rather than swallowing silently.
            log.warn("Failed to attach screenshot [{}] to Extent report: {}", screenshotPath, e.getMessage(), e);
        }
    }
}
