package com.company.framework.base;

import com.company.framework.driver.DriverFactory;
import com.company.framework.driver.DriverManager;
import com.company.framework.utils.PropertyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.lang.reflect.Method;

/**
 * Base class every test class extends. Owns exactly one responsibility:
 * the WebDriver lifecycle around each test method (Single Responsibility
 * Principle) - create a fresh, isolated browser session before each test,
 * navigate to the application under test, and tear the session down
 * afterwards, regardless of pass/fail.
 *
 * <p><b>Deliberately NOT this class's job:</b> screenshot capture, Extent
 * Report logging, or pass/fail status handling. That all lives in
 * {@code TestListener} (Phase 6) via TestNG's {@code ITestListener} hooks,
 * which fire for every test automatically - duplicating that logic here
 * (e.g. a try/catch per test method) would mean every new test class has to
 * remember to wire it up correctly. Centralizing it in one listener means
 * it is applied uniformly and can never be forgotten.
 *
 * <pre>
 * Lifecycle:
 *   {@literal @}BeforeSuite   → log suite start
 *   {@literal @}BeforeMethod  → DriverFactory.initDriver() → navigate to configured URL
 *   ... test method runs (via Page Objects) ...
 *   {@literal @}AfterMethod   → DriverFactory.quitDriver()   (alwaysRun = true: fires even on failure/skip)
 *   {@literal @}AfterSuite    → log suite finish
 * </pre>
 */
public abstract class BaseTest {

    protected static final Logger log = LogManager.getLogger(BaseTest.class);

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        log.info("=========================================================");
        log.info(" TEST SUITE EXECUTION STARTED | environment=[{}] | browser=[{}]",
                PropertyUtils.getEnvironment(), PropertyUtils.getBrowser());
        log.info("=========================================================");
    }

    /**
     * Runs before every {@code @Test} method, on whichever thread TestNG
     * assigns it to (relevant under {@code parallel="methods"}/{@code "tests"}).
     * Each invocation gets its own WebDriver via {@link DriverManager}'s
     * ThreadLocal storage - there is no shared state between concurrently
     * running test methods.
     *
     * @param method injected by TestNG; used only for a readable log line
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        log.info("---- Starting test [{}] on thread [{}] ----", method.getName(), Thread.currentThread().getId());
        DriverFactory.initDriver();
        navigateToApplication();
    }

    private void navigateToApplication() {
        String url = PropertyUtils.getUrl();
        log.info("Navigating to application URL: [{}]", url);
        DriverManager.getDriver().get(url);
    }

    /**
     * {@code alwaysRun = true} is essential here: TestNG skips normal
     * {@code @AfterMethod}s when a {@code @BeforeMethod} throws, but a
     * WebDriver that was successfully created in {@code setUp()} must still be
     * quit even if the test method itself fails or throws - otherwise every
     * failure leaks a browser process, and a long regression run on a CI
     * agent eventually exhausts memory/handles.
     */
//    @AfterMethod(alwaysRun = true)
//    public void tearDown() {
//        DriverFactory.quitDriver();
//        log.info("---- Finished test on thread [{}], WebDriver session closed ----", Thread.currentThread().getId());
//    }
//
//    @AfterSuite(alwaysRun = true)
//    public void afterSuite() {
//        log.info("=========================================================");
//        log.info(" TEST SUITE EXECUTION FINISHED");
//        log.info("=========================================================");
//    }
}
