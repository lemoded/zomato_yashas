package com.company.framework.testdata;

import com.company.framework.utils.ExcelUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;

/**
 * Centralized home for every reusable {@code @DataProvider} in the framework.
 *
 * <p>Keeping data providers here - rather than as private methods scattered
 * inside individual test classes - means the same data set can be shared
 * across multiple test classes (e.g. a future {@code CheckoutTest} reusing
 * {@link #loginData()} to get to a logged-in state) and means adding a new
 * data-driven data set never requires touching an existing test class.
 *
 * <p>Methods are {@code static} so any test class can reference this
 * provider without needing an instance:
 * <pre>{@code
 * @Test(dataProvider = "loginData", dataProviderClass = DataProviderUtils.class)
 * public void login(String username, String password, String expected) { ... }
 * }</pre>
 */
public final class DataProviderUtils {

    private static final Logger log = LogManager.getLogger(DataProviderUtils.class);

    private DataProviderUtils() {
        throw new UnsupportedOperationException("DataProviderUtils is a static utility class and cannot be instantiated");
    }

    /**
     * Supplies every row of the {@code LoginData} sheet in
     * {@code TestData.xlsx} as {@code {username, password, expected}} triples.
     */
    @DataProvider(name = "loginData")
    public static Object[][] loginData() {
        Object[][] data = ExcelUtils.getData("LoginData");
        log.info("loginData DataProvider supplying [{}] row(s)", data.length);
        return data;
    }
}
