package com.company.tests;

import com.company.framework.base.BaseTest;
import com.company.framework.pages.HomePage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SampleTest extends BaseTest {

    @Test
    public void validLogin() {

        HomePage homepage = new HomePage();
        homepage.clickSearchBox();
        
    }
}
