package com.company.framework.base;

import com.company.framework.driver.DriverManager;
import com.company.framework.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

/**
 * Base class for every Page Object. Wraps raw Selenium calls with explicit
 * waits ({@link WaitUtils}) and logging, so individual page objects
 * (LoginPage, HomePage, ...) never talk to {@link WebElement} directly
 * without synchronization.
 *
 * <p><b>Layering (per framework architecture):</b>
 * <pre>
 * Test → Page Object → BasePage → WaitUtils / DriverManager → WebDriver
 * </pre>
 * Page objects extend this class and get a small, curated set of primitives
 * (click, sendKeys, getText, ...) to build their own business-facing methods
 * (e.g. {@code LoginPage.login(user, pass)}) on top of.
 *
 * <p><b>This class intentionally contains zero test-specific or
 * business logic</b> - no assertions, no test data, no navigation to specific
 * pages. Its only job is "safe, reusable Selenium primitives" (Single
 * Responsibility Principle). Assertions belong in test classes; page-specific
 * flows belong in the page object subclass.
 *
 * <p>All methods are {@code protected} instance methods (not static) so
 * subclasses inherit them naturally via {@code extends BasePage} while
 * keeping them invisible to test classes that only hold a page object
 * reference - tests should call {@code loginPage.login(...)}, never
 * {@code loginPage.click(...)} directly.
 */
public abstract class BasePage {

    protected static final Logger log = LogManager.getLogger(BasePage.class);

    /** Always fetches the current thread's driver fresh - never cache this in a field. */
    protected WebDriver driver() {
        return DriverManager.getDriver();
    }

    // ===================== Click =====================

    protected void click(WebElement element) {
        WaitUtils.waitForElementClickable(element).click();
        log.debug("Clicked element: [{}]", describeElement(element));
    }

    protected void click(By locator) {
        WaitUtils.waitForElementClickable(locator).click();
        log.debug("Clicked element located by: [{}]", locator);
    }

    // ===================== Send keys =====================

    protected void sendKeys(WebElement element, String text) {
        WebElement visibleElement = WaitUtils.waitForElementVisible(element);
        visibleElement.clear();
        visibleElement.sendKeys(text);
        log.debug("Entered text into element: [{}]", describeElement(element));
    }

    protected void sendKeys(By locator, String text) {
        WebElement visibleElement = WaitUtils.waitForElementVisible(locator);
        visibleElement.clear();
        visibleElement.sendKeys(text);
        log.debug("Entered text into element located by: [{}]", locator);
    }

    // ===================== Read state =====================

    protected String getText(WebElement element) {
        return WaitUtils.waitForElementVisible(element).getText();
    }

    protected String getText(By locator) {
        return WaitUtils.waitForElementVisible(locator).getText();
    }

    protected String getAttribute(WebElement element, String attributeName) {
        return WaitUtils.waitForElementVisible(element).getAttribute(attributeName);
    }

    /**
     * Non-waiting existence/visibility check - deliberately does NOT use
     * WaitUtils, since "is this displayed right now" (e.g. for a conditional
     * branch in a page object) is semantically different from "wait until this
     * becomes visible". Returns {@code false} rather than throwing when the
     * element isn't present/attached - that IS the meaningful answer to
     * "is it displayed", not an error to propagate.
     */
    protected boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            log.debug("Element not displayed / not present in DOM: {}", e.getMessage());
            return false;
        }
    }

    protected boolean isEnabled(WebElement element) {
        return WaitUtils.waitForElementVisible(element).isEnabled();
    }

    protected boolean isSelected(WebElement element) {
        return WaitUtils.waitForElementVisible(element).isSelected();
    }

    protected void clear(WebElement element) {
        WaitUtils.waitForElementVisible(element).clear();
    }

    protected String getTitle() {
        return driver().getTitle();
    }

    protected String getCurrentUrl() {
        return driver().getCurrentUrl();
    }

    // ===================== JavaScript-assisted actions =====================

    /**
     * Scrolls the element into the viewport center via JS. Useful before
     * interacting with elements that a sticky header/footer would otherwise
     * obscure, or that a native {@code click()} reports as
     * "element not interactable" despite being technically present.
     */
    protected void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver()).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        log.debug("Scrolled to element: [{}]", describeElement(element));
    }

    /**
     * Clicks via JavaScript rather than a native WebDriver click. Reserved for
     * elements that are logically clickable but obstructed (e.g. by an
     * overlay/animation) - overusing this hides real UI bugs a native click
     * would have caught, so prefer {@link #click(WebElement)} by default.
     */
    protected void javaScriptClick(WebElement element) {
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
        log.debug("JavaScript-clicked element: [{}]", describeElement(element));
    }

    protected void mouseHover(WebElement element) {
        new Actions(driver()).moveToElement(WaitUtils.waitForElementVisible(element)).perform();
        log.debug("Hovered over element: [{}]", describeElement(element));
    }

    // ===================== Dropdowns =====================

    protected void selectDropdownByVisibleText(WebElement dropdown, String visibleText) {
        new Select(WaitUtils.waitForElementVisible(dropdown)).selectByVisibleText(visibleText);
        log.debug("Selected dropdown option by visible text: [{}]", visibleText);
    }

    protected void selectDropdownByValue(WebElement dropdown, String value) {
        new Select(WaitUtils.waitForElementVisible(dropdown)).selectByValue(value);
        log.debug("Selected dropdown option by value: [{}]", value);
    }

    protected void selectDropdownByIndex(WebElement dropdown, int index) {
        new Select(WaitUtils.waitForElementVisible(dropdown)).selectByIndex(index);
        log.debug("Selected dropdown option by index: [{}]", index);
    }

    protected void clickWithJsFallback(WebElement element) {
        try {
            click(element);
        } catch (ElementClickInterceptedException e) {
            log.warn("Native click intercepted for [{}], retrying via JavaScript click: {}",
                    describeElement(element), e.getMessage());
            scrollToElement(element);
            javaScriptClick(element);
        }
    }

    // ===================== Internal =====================

    /** Best-effort human-readable element description for log lines only - never used for assertions. */
    private String describeElement(WebElement element) {
        try {
            return element.toString();
        } catch (StaleElementReferenceException e) {
            return "stale-element";
        }
    }
}
