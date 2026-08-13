package com.company.framework.utils;

import com.company.framework.constants.FrameworkConstants;
import com.company.framework.driver.DriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures browser screenshots for failure diagnostics and Extent Report attachments.
 *
 * <p>Filenames follow {@code <TestName>_<yyyyMMdd_HHmmss>.png} (e.g.
 * {@code LoginTest_validLogin_20260808_103020.png}) so screenshots sort
 * chronologically and are traceable back to the exact test run without
 * opening the report.
 *
 * <p>This class only handles image capture/storage - it has no knowledge of
 * TestNG, ExtentReports, or pass/fail state (Single Responsibility Principle).
 * {@code TestListener} (Phase 6) decides <i>when</i> to call this; ExtentReport
 * classes decide how to display the returned path.
 */
public final class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern(FrameworkConstants.DATE_FORMAT_PATTERN);

    private ScreenshotUtils() {
        throw new UnsupportedOperationException("ScreenshotUtils is a static utility class and cannot be instantiated");
    }

    /**
     * Captures a screenshot of the current thread's browser session and saves it
     * under {@link FrameworkConstants#SCREENSHOT_PATH}.
     *
     * @param testName logical test/step name used to build the filename (e.g. "LoginTest_validLogin")
     * @return the absolute path of the saved screenshot file, or {@code null} if
     *         capture failed - callers (e.g. TestListener) must null-check before
     *         attaching to a report rather than assuming success.
     */
    public static String captureScreenshot(String testName) {
        WebDriver driver;
        try {
            driver = DriverManager.getDriver();
        } catch (IllegalStateException e) {
            log.warn("Cannot capture screenshot for [{}] - no active WebDriver on thread [{}]",
                    testName, Thread.currentThread().getId());
            return null;
        }

        if (!(driver instanceof TakesScreenshot)) {
            log.warn("Driver implementation [{}] does not support screenshots", driver.getClass().getName());
            return null;
        }

        try {
            ensureScreenshotDirectoryExists();

            String fileName = sanitizeFileName(testName) + "_" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".png";
            File sourceFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destinationFile = new File(FrameworkConstants.SCREENSHOT_PATH + fileName);

            FileUtils.copyFile(sourceFile, destinationFile);
            log.info("Screenshot captured for [{}] -> [{}]", testName, destinationFile.getAbsolutePath());
            return destinationFile.getAbsolutePath();

        } catch (IOException e) {
            log.error("Failed to capture/save screenshot for test [{}]: {}", testName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extent Reports embeds screenshots best via a path relative to the report
     * HTML file (so the report folder stays portable/zippable for sharing).
     * Falls back to the absolute path if relativization fails for any reason.
     */
    public static String getRelativePathForReport(String absoluteScreenshotPath) {
        if (absoluteScreenshotPath == null) {
            return null;
        }
        try {
            Path reportDir = Paths.get(FrameworkConstants.REPORT_PATH);
            Path screenshotPath = Paths.get(absoluteScreenshotPath);
            return reportDir.relativize(screenshotPath).toString().replace("\\", "/");
        } catch (IllegalArgumentException e) {
            log.debug("Could not relativize screenshot path [{}] against report dir - using absolute path",
                    absoluteScreenshotPath);
            return absoluteScreenshotPath;
        }
    }

    private static void ensureScreenshotDirectoryExists() throws IOException {
        Path dir = Paths.get(FrameworkConstants.SCREENSHOT_PATH);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            log.debug("Created screenshot directory: [{}]", dir);
        }
    }

    private static String sanitizeFileName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "UnknownTest";
        }
        // Strip anything that isn't filesystem-safe across Windows/Linux/Mac CI agents.
        return rawName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
