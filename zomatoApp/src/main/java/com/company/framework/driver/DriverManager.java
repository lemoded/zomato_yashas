package com.company.framework.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Thread-safe holder for the active {@link WebDriver} instance.
 *
 * <p><b>Why ThreadLocal instead of a static WebDriver field?</b>
 * TestNG's {@code parallel="tests"}/{@code "methods"} execution runs multiple
 * tests concurrently on separate threads. A single {@code static WebDriver}
 * field would be overwritten by whichever thread runs last, causing tests on
 * other threads to silently drive the wrong browser session (or a closed
 * one). {@link ThreadLocal} gives each thread its own isolated WebDriver
 * reference, so thread A's tests only ever see thread A's browser.
 *
 * <p>This class deliberately holds <b>no test logic</b> — it is a pure
 * accessor/lifecycle wrapper (Single Responsibility Principle). Browser
 * creation lives in {@link DriverFactory}; navigation/assertions live in
 * page objects and tests.
 */
public final class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);

    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    private DriverManager() {
        throw new UnsupportedOperationException("DriverManager is a static utility class and cannot be instantiated");
    }

    /**
     * Returns the WebDriver instance bound to the calling thread.
     *
     * @throws IllegalStateException if no driver has been initialized on this thread yet -
     *                                this fails loudly instead of returning null, which would
     *                                otherwise surface as a confusing NullPointerException deep
     *                                inside a page object.
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver has not been initialized on thread [" + Thread.currentThread().getId()
                            + "]. Ensure DriverFactory.initDriver() runs (typically in @BeforeMethod) "
                            + "before any page object or test step executes.");
        }
        return driver;
    }

    /**
     * Binds a WebDriver instance to the calling thread. Called once by
     * {@link DriverFactory#initDriver()} per test method.
     */
    public static void setDriver(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Cannot bind a null WebDriver instance to DriverManager");
        }
        DRIVER_THREAD_LOCAL.set(driver);
        log.debug("WebDriver bound to thread [{}]", Thread.currentThread().getId());
    }

    /** @return true if a WebDriver is currently bound to the calling thread. */
    public static boolean isDriverInitialized() {
        return DRIVER_THREAD_LOCAL.get() != null;
    }

    /**
     * Gracefully quits the browser session bound to this thread (closes all
     * windows, ends the driver process) and clears the ThreadLocal reference.
     * Safe to call even if no driver was initialized - it simply no-ops.
     *
     * <p>Always call this in {@code @AfterMethod}, even on test failure -
     * otherwise browser/driver processes accumulate and eventually exhaust
     * CI agent memory/handles across a long regression run.
     */
    public static void unloadDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver == null) {
            log.debug("unloadDriver() called on thread [{}] with no active driver - nothing to do",
                    Thread.currentThread().getId());
            return;
        }
        try {
            driver.quit();
            log.info("WebDriver session quit successfully on thread [{}]", Thread.currentThread().getId());
        } catch (Exception e) {
            // Never let cleanup failure mask the original test failure, but never swallow silently either.
            log.error("Exception while quitting WebDriver on thread [{}]: {}",
                    Thread.currentThread().getId(), e.getMessage(), e);
        } finally {
            remove();
        }
    }

    /**
     * Removes the ThreadLocal reference without quitting the browser.
     * Exposed separately from {@link #unloadDriver()} so callers that manage
     * quitting elsewhere can still guarantee ThreadLocal cleanup (preventing
     * memory leaks in thread-pooled execution, e.g. TestNG's parallel thread
     * pool reusing OS threads across test methods).
     */
    public static void remove() {
        DRIVER_THREAD_LOCAL.remove();
        log.debug("ThreadLocal WebDriver reference cleared for thread [{}]", Thread.currentThread().getId());
    }
}
