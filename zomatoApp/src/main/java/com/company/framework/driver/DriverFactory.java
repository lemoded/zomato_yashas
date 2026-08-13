package com.company.framework.driver;

import com.company.framework.utils.PropertyUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * Owns creation and low-level configuration of {@link WebDriver} instances.
 *
 * <p>Responsibilities (and only these - Single Responsibility Principle):
 * <ul>
 *     <li>resolve which browser to launch (config file, overridable via {@code -Dbrowser=...})</li>
 *     <li>build browser-specific {@code *Options} (headless, window size, stability flags)</li>
 *     <li>apply timeouts (page load, script)</li>
 *     <li>hand the created driver to {@link DriverManager} for thread-safe storage</li>
 * </ul>
 *
 * <p>It does <b>not</b> know about tests, page objects, or reporting - those layers depend on
 * this one, never the other way around (Dependency Inversion in the layering sense: higher
 * layers depend on this stable abstraction, this class doesn't reach upward).
 *
 * <p><b>Configuration precedence</b> (delegated to {@link PropertyUtils#getBrowser()} etc.):
 * a JVM system property such as {@code -Dbrowser=firefox} (passed by the Maven Surefire plugin,
 * see {@code pom.xml}'s {@code systemPropertyVariables}) always overrides the value in
 * {@code config.properties}. This is what makes {@code mvn test -Dbrowser=firefox} work without
 * touching any file.
 */
public final class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {
        throw new UnsupportedOperationException("DriverFactory is a static utility class and cannot be instantiated");
    }

    /**
     * Creates a new WebDriver for the configured browser, applies timeouts,
     * maximizes the window (skipped when headless), and binds it to the
     * calling thread via {@link DriverManager}.
     *
     * <p>Intended to be called once per test method from {@code BaseTest#setUp()}
     * (Phase 5), so every test method - even when run in parallel - gets its
     * own isolated browser session.
     */
    public static void initDriver() {
        String browserName = PropertyUtils.getBrowser();
        boolean headless = PropertyUtils.isHeadless();

        log.info("Initializing WebDriver | browser=[{}] | headless=[{}] | thread=[{}]",
                browserName, headless, Thread.currentThread().getId());

        BrowserType browserType = BrowserType.fromString(browserName);
        WebDriver driver = createDriver(browserType, headless);

        applyTimeouts(driver);
        if (!headless) {
            driver.manage().window().maximize();
        }

        DriverManager.setDriver(driver);
        log.info("WebDriver ready | browser=[{}] | thread=[{}]", browserName, Thread.currentThread().getId());
    }

    /**
     * Browser dispatch. To add a new browser (e.g. Safari): add a constant to
     * {@link BrowserType}, add one {@code case} here, add one {@code create*Driver()}
     * method - no other class in the framework changes (Open/Closed Principle).
     */
    private static WebDriver createDriver(BrowserType browserType, boolean headless) {
        switch (browserType) {
            case CHROME:
                return createChromeDriver(headless);
            case FIREFOX:
                return createFirefoxDriver(headless);
            case EDGE:
                return createEdgeDriver(headless);
            default:
                // Unreachable given BrowserType.fromString()'s validation, but keeps
                // the switch exhaustive and safe against future enum additions.
                throw new IllegalArgumentException("No driver creation logic mapped for browser: " + browserType);
        }
    }

    /**
     * Creates a Chrome session with basic bot-detection countermeasures applied
     * (see inline comments for what/why).
     *
     * <p><b>Be realistic about what this does and doesn't solve:</b> these
     * countermeasures defeat {@code navigator.webdriver} checks and the most
     * common ChromeDriver "tells" - enough for many sites, and enough to make
     * "works manually, fails via Selenium" match again on plenty of targets.
     * They do <b>not</b> defeat a fully-configured enterprise bot manager
     * (Akamai Bot Manager, Cloudflare Bot Management, PerimeterX, etc.), which
     * can also fingerprint TLS handshake order (JA3), canvas/WebGL rendering
     * output, mouse-movement/timing entropy, and other signals this class has
     * no access to. If a target site still blocks after this, that is very
     * likely a deliberate anti-automation decision on their part, not a
     * remaining misconfiguration here - the appropriate next step is usually a
     * sanctioned staging/QA environment or API-level test access, not deeper
     * evasion.
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        // Stability flags recommended for CI/containerized execution (Jenkins agents,
        // Docker) where a full desktop session / shared memory isn't guaranteed.
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        // ===================== Bot-detection countermeasures =====================
        // Sites behind Akamai Bot Manager / Cloudflare Bot Management / similar WAFs
        // fingerprint automation-controlled Chrome and block it even when a manual
        // visit from the same IP succeeds. These reduce the most common, well-known
        // tells. This is NOT a guarantee against a fully-configured bot manager -
        // see the Javadoc on this method for the honest limits of this approach.
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", java.util.List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        ChromeDriver driver = new ChromeDriver(options);

        // ChromeOptions above stop Chrome from launching with the obvious "I am
        // automated" markers, but navigator.webdriver still reports `true` by
        // default even with those flags set. This CDP command patches that
        // property out at the JS-engine level before any page script (including
        // the bot manager's own detection script) runs on every new document.
        java.util.Map<String, Object> cdpParams = new java.util.HashMap<>();
        cdpParams.put("source",
                "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });");
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", cdpParams);

        return driver;
    }

    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("-headless");
            options.addArguments("--width=1920");
            options.addArguments("--height=1080");
        }

        return new FirefoxDriver(options);
    }

    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();

        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        return new EdgeDriver(options);
    }

    /**
     * Applies page-load and script timeouts read from configuration.
     * Implicit wait is intentionally left at the configured value (default 0) -
     * see {@link com.company.framework.utils.WaitUtils} (Phase 3) for why mixing
     * implicit and explicit waits is avoided in this framework.
     */
    private static void applyTimeouts(WebDriver driver) {
        Duration pageLoadTimeout = Duration.ofSeconds(PropertyUtils.getPageLoadTimeout());
        Duration scriptTimeout = Duration.ofSeconds(PropertyUtils.getScriptTimeout());
        Duration implicitWait = Duration.ofSeconds(PropertyUtils.getImplicitWait());

        driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
        driver.manage().timeouts().scriptTimeout(scriptTimeout);
        driver.manage().timeouts().implicitlyWait(implicitWait);

        log.debug("Timeouts applied | pageLoad=[{}s] | script=[{}s] | implicit=[{}s]",
                pageLoadTimeout.toSeconds(), scriptTimeout.toSeconds(), implicitWait.toSeconds());
    }

    /**
     * Convenience wrapper so BaseTest doesn't need to import DriverManager directly
     * just to tear down - keeps the "quit" concept discoverable from the same class
     * that created the driver.
     */
    public static void quitDriver() {
        DriverManager.unloadDriver();
    }
}
