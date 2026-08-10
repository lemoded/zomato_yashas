# Selenium Java Automation Framework

Production-grade Selenium WebDriver automation framework built with **Java 21 + Maven + TestNG**,
designed for real QA automation teams: thread-safe parallel execution, externalized configuration,
data-driven testing, rich HTML reporting, and CI/CD-ready execution.

## Tech Stack

| Tool | Purpose |
|---|---|
| Selenium WebDriver 4.24 | Browser automation |
| TestNG 7.10 | Test execution, DataProvider, parallel execution, listeners |
| WebDriverManager 5.9 | Automatic browser driver resolution |
| ExtentReports 5.1 | HTML test reporting |
| Log4j2 2.23 | Structured logging |
| Apache POI 5.3 | Excel-driven test data |
| Maven Surefire | Test execution + CLI parameter passing |

## Project Structure

```
selenium-java-framework/
├── pom.xml
├── testng.xml
├── reports/                 ExtentReport.html lands here
├── screenshots/              failure screenshots land here
├── logs/                     automation.log / error.log land here
└── src/
    ├── main/java/com/company/framework/
    │   ├── constants/        FrameworkConstants - all paths/defaults, one place
    │   ├── driver/            DriverManager (ThreadLocal), DriverFactory, BrowserType
    │   ├── utils/             PropertyUtils, WaitUtils, ScreenshotUtils, ExcelUtils
    │   ├── reporting/         ExtentManager, ExtentReport (ThreadLocal)
    │   ├── base/              BasePage, BaseTest
    │   ├── listeners/         TestListener (ITestListener)
    │   ├── pages/             LoginPage, HomePage, DashboardPage
    │   └── testdata/          DataProviderUtils
    ├── main/resources/
    │   ├── config.properties, config-qa/uat/prod.properties
    │   ├── log4j2.xml
    │   └── testdata/TestData.xlsx
    └── test/java/com/company/tests/
        └── LoginTest.java
```

## Prerequisites

- JDK 21+
- Maven 3.9+
- Chrome, Firefox, and/or Edge installed locally (WebDriverManager handles the driver binaries automatically)

## Running the tests

```bash
# Default: chrome, qa environment, config.properties values
mvn clean test

# Override browser
mvn clean test -Dbrowser=firefox
mvn clean test -Dbrowser=edge

# Headless
mvn clean test -Dheadless=true

# Override environment (loads config-<env>.properties as an overlay)
mvn clean test -Denvironment=uat

# Combine
mvn clean test -Dbrowser=chrome -Dheadless=true -Denvironment=qa

# Or via a Maven profile
mvn clean test -Puat
```

Configuration precedence (highest to lowest): **JVM system property (`-D...`)** → **`config-<environment>.properties` overlay** → **`config.properties` base defaults**.

## Sample app

The framework is pre-configured against the public [Sauce Labs demo app](https://www.saucedemo.com)
so `LoginTest` runs end-to-end out of the box. `TestData.xlsx`'s `LoginData` sheet uses that app's
published demo credentials (`standard_user` / `secret_sauce`, etc.). Point `config.properties` and
the page objects under `pages/` at your own application when adapting this framework.

## What LoginTest covers

| Test | Group | Source of data |
|---|---|---|
| `validLogin` | smoke | `TestData.xlsx` row 1 |
| `invalidLogin` | smoke | `TestData.xlsx` row 3 |
| `emptyUsername` | smoke | `TestData.xlsx` row 4 |
| `emptyPassword` | smoke | `TestData.xlsx` row 5 |
| `loginScenarios` (data-driven) | regression | `DataProviderUtils.loginData()` → all rows |

`testng.xml` runs the `smoke` and `regression` groups as two parallel `<test>` tags
(`parallel="tests" thread-count="2"`) - each gets its own isolated browser session.

## Reports, screenshots, logs

After a run:
- `reports/ExtentReport.html` - open in any browser; includes system info, pass/fail/skip per test, and attached failure screenshots
- `screenshots/<TestClass>_<testMethod>_<timestamp>.png` - captured automatically on failure via `TestListener`
- `logs/automation.log` - full run log (rolls daily / at 10MB)
- `logs/error.log` - ERROR-level only, for fast CI triage

## CI/CD (Jenkins)

```bash
mvn clean test -Dbrowser=chrome -Dheadless=true -Denvironment=qa -DthreadCount=4 -DsuiteXmlFile=testng.xml
```

Archive `reports/`, `screenshots/`, and `logs/` as Jenkins post-build artifacts. The build fails
(non-zero exit code) on any test failure, driven by Surefire's default failure behavior - no
custom exit-code handling needed.

## Extending the framework

- **New browser**: add a constant to `BrowserType`, add a `case` + `create*Driver()` method in `DriverFactory`. Nothing else changes.
- **New page**: create a class extending `BasePage`, use `@FindBy` + `PageFactory.initElements(driver(), this)` in the constructor, expose only business methods.
- **New data-driven test**: add a method to `DataProviderUtils`, add a sheet to `TestData.xlsx`.
- **New environment**: add `config-<env>.properties` with just the keys that differ from the base.
