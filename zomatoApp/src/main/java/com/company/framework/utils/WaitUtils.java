package com.company.framework.utils;

import com.company.framework.driver.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Centralized explicit-wait utility. Every synchronization point in the
 * framework - page objects, base classes, tests - must go through this
 * class rather than calling {@code Thread.sleep()} or building ad-hoc
 * {@code WebDriverWait} instances inline.
 *
 * <p><b>Why explicit wait over implicit wait?</b>
 * Implicit wait (set once via {@code driver.manage().timeouts().implicitlyWait(...)})
 * applies the same blanket delay to <i>every</i> {@code findElement} call for the
 * life of the driver, regardless of what condition actually matters at that point
 * in the test - visibility, clickability, and mere DOM presence are very different
 * conditions, and a single implicit timeout can't distinguish them. Worse, mixing
 * implicit and explicit waits in the same driver session is a well-documented
 * Selenium anti-pattern: the effective wait time becomes the sum/interleaving of
 * both, producing unpredictable, hard-to-reproduce timeouts. This framework
 * therefore keeps implicit wait at {@code 0} (see {@code DriverFactory}) and relies
 * exclusively on explicit/fluent waits, each targeting the exact condition the
 * step actually needs (visible, clickable, invisible, present, ...).
 *
 * <p>Every method here pulls the driver from {@link DriverManager#getDriver()}
 * on each call rather than caching it, so this class is inherently safe to call
 * from any thread during parallel execution - there is no shared mutable state.
 */
public final class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);

    private WaitUtils() {
        throw new UnsupportedOperationException("WaitUtils is a static utility class and cannot be instantiated");
    }

    private static WebDriverWait getWait() {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(PropertyUtils.getExplicitWait()));
    }

    // ===================== Visibility =====================

    public static WebElement waitForElementVisible(WebElement element) {
        log.debug("Waiting for element visibility (WebElement overload)");
        return getWait().until(ExpectedConditions.visibilityOf(element));
    }

    public static WebElement waitForElementVisible(By locator) {
        log.debug("Waiting for element visibility: [{}]", locator);
        return getWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // ===================== Clickability =====================

    public static WebElement waitForElementClickable(WebElement element) {
        log.debug("Waiting for element to be clickable (WebElement overload)");
        return getWait().until(ExpectedConditions.elementToBeClickable(element));
    }

    public static WebElement waitForElementClickable(By locator) {
        log.debug("Waiting for element to be clickable: [{}]", locator);
        return getWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    // ===================== Presence =====================

    public static WebElement waitForPresenceOfElement(By locator) {
        log.debug("Waiting for element presence in DOM: [{}]", locator);
        return getWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static List<WebElement> waitForPresenceOfAllElements(By locator) {
        log.debug("Waiting for presence of all elements: [{}]", locator);
        return getWait().until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    // ===================== Invisibility =====================

    public static boolean waitForInvisibilityOfElement(By locator) {
        log.debug("Waiting for element invisibility: [{}]", locator);
        return getWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForInvisibilityOfElement(WebElement element) {
        log.debug("Waiting for element invisibility (WebElement overload)");
        return getWait().until(ExpectedConditions.invisibilityOf(element));
    }

    // ===================== Alerts =====================

    public static Alert waitForAlertPresent() {
        log.debug("Waiting for JavaScript alert to be present");
        return getWait().until(ExpectedConditions.alertIsPresent());
    }

    // ===================== Title / URL =====================

    public static boolean waitForTitleContains(String titleFragment) {
        log.debug("Waiting for page title to contain: [{}]", titleFragment);
        return getWait().until(ExpectedConditions.titleContains(titleFragment));
    }

    public static boolean waitForUrlContains(String urlFragment) {
        log.debug("Waiting for URL to contain: [{}]", urlFragment);
        return getWait().until(ExpectedConditions.urlContains(urlFragment));
    }

    // ===================== Fluent wait =====================

    /**
     * General-purpose fluent wait for conditions not covered by the convenience
     * methods above (e.g. waiting on a custom {@link Function} against the driver,
     * polling a JS-rendered value, or waiting past transient
     * {@link StaleElementReferenceException}s during a DOM re-render).
     *
     * @param condition       function evaluated against the WebDriver until it returns a non-null,
     *                        non-false result
     * @param timeoutSeconds  total time to wait before giving up
     * @param pollingMillis   interval between condition evaluations
     */
    public static <T> T fluentWait(Function<WebDriver, T> condition, long timeoutSeconds, long pollingMillis) {
        Wait<WebDriver> wait = new FluentWait<>(DriverManager.getDriver())
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollingMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        try {
            return wait.until(condition);
        } catch (TimeoutException e) {
            log.error("Fluent wait timed out after [{}s] (polling every [{}ms])", timeoutSeconds, pollingMillis);
            throw e;
        }
    }

    /** Fluent wait overload using the framework's configured explicit wait and default polling interval. */
    public static <T> T fluentWait(Function<WebDriver, T> condition) {
        return fluentWait(condition,
                PropertyUtils.getExplicitWait(),
                com.company.framework.constants.FrameworkConstants.FLUENT_WAIT_POLLING_MILLIS);
    }
}
