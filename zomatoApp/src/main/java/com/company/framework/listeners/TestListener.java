package com.company.framework.listeners;

import com.company.framework.reporting.ExtentManager;
import com.company.framework.reporting.ExtentReport;
import com.company.framework.utils.PropertyUtils;
import com.company.framework.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Central TestNG lifecycle hook that wires driver execution to reporting -
 * this is the ONLY place in the framework that decides "what happens on
 * pass/fail/skip", so that behavior is guaranteed for every test class
 * without any of them writing try/catch/screenshot boilerplate themselves.
 *
 * <p>Registered globally via {@code testng.xml}:
 * <pre>{@code
 * <listeners>
 *     <listener class-name="com.company.framework.listeners.TestListener"/>
 * </listeners>
 * }</pre>
 *
 * <p><b>Thread safety:</b> TestNG instantiates a single {@code TestListener}
 * instance and invokes its callbacks from whichever thread is running that
 * particular test (relevant under {@code parallel="methods"}/{@code "tests"}).
 * This class holds <b>no instance state</b> - every collaborator it calls
 * ({@link ExtentReport}, {@link ScreenshotUtils}, {@code DriverManager}) is
 * itself backed by {@link ThreadLocal}, so concurrent callback invocations
 * from different threads never interfere with each other despite sharing
 * this one listener object.
 *
 * <p>Failure handling flow (matches the framework's required diagram):
 * <pre>
 * Test Failure → onTestFailure() → capture screenshot → attach to Extent
 *              → log exception → mark FAILED in report → test run continues
 * </pre>
 */
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    // ===================== Suite-level hooks =====================

    @Override
    public void onStart(ITestContext context) {
        log.info("TestListener attached | test-tag=[{}] | environment=[{}] | browser=[{}]",
                context.getName(), PropertyUtils.getEnvironment(), PropertyUtils.getBrowser());
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("Test tag [{}] finished | passed=[{}] failed=[{}] skipped=[{}]",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());

        // Flushed here rather than in BaseTest: onFinish() is the true end of
        // this <test> tag's execution across ALL of its threads, whereas
        // BaseTest's @AfterSuite/@AfterMethod only know about a single test
        // method's lifecycle. Flushing exactly once here guarantees every
        // buffered Extent log entry - from every parallel thread - is on disk
        // before the JVM exits.
        ExtentManager.flush();
    }

    // ===================== Per-test hooks =====================

    @Override
    public void onTestStart(ITestResult result) {
        String testName = getTestName(result);
        String description = getTestDescription(result);
        ExtentReport.createTest(testName, description);
        log.info("Test started: [{}]", testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = getTestName(result);
        long durationMillis = result.getEndMillis() - result.getStartMillis();
        ExtentReport.pass("Test passed in " + durationMillis + " ms");
        log.info("Test PASSED: [{}] ({} ms)", testName, durationMillis);
        ExtentReport.removeTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = getTestName(result);
        Throwable cause = result.getThrowable();
        String failureReason = cause != null ? cause.getMessage() : "No exception message available";

        log.error("Test FAILED: [{}] - {}", testName, failureReason, cause);

        // 1. Capture screenshot
        String screenshotPath = ScreenshotUtils.captureScreenshot(getScreenshotBaseName(result));

        // 2. Attach screenshot to Extent Report (no-ops gracefully if capture failed above)
        ExtentReport.attachScreenshot(screenshotPath);

        // 3. Log exception into the report
        ExtentReport.fail("Test failed: " + failureReason);
        if (cause != null) {
            ExtentReport.fail(stackTraceToString(cause));
        }

        // 4. Report already marked FAILED via the calls above; TestNG itself
        //    marks the test result FAILED independently of this listener.
        ExtentReport.removeTest();

        // 5. Execution continues - TestNG proceeds to the next test method automatically;
        //    this listener never throws or halts the suite.
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = getTestName(result);
        Throwable cause = result.getThrowable();
        String reason = cause != null ? cause.getMessage() : "Skipped (dependency failure or explicit skip)";

        ExtentReport.skip("Test skipped: " + reason);
        log.warn("Test SKIPPED: [{}] - {}", testName, reason);
        ExtentReport.removeTest();
    }

    // ===================== Helpers =====================

    /** e.g. "LoginTest.validLogin" - used as the Extent report node title and in log lines. */
    private String getTestName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }

    /** e.g. "LoginTest_validLogin" - used as the screenshot filename base, matching FrameworkConstants convention. */
    private String getScreenshotBaseName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "_" + result.getMethod().getMethodName();
    }

    /** Falls back to the method name if no {@code @Test(description = "...")} was supplied. */
    private String getTestDescription(ITestResult result) {
        String description = result.getMethod().getDescription();
        return (description == null || description.isBlank()) ? result.getMethod().getMethodName() : description;
    }

    private String stackTraceToString(Throwable throwable) {
        StringBuilder sb = new StringBuilder(throwable.toString());
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;at ").append(element);
            // Cap stack depth in the HTML report - the full trace is already in automation.log/error.log;
            // the report only needs enough context to identify the failure point at a glance.
            if (sb.length() > 2000) {
                sb.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;... see logs/error.log for full stack trace");
                break;
            }
        }
        return sb.toString();
    }
}
