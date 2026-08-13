package com.company.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.company.framework.base.BaseTest;
import com.company.framework.pages.HomePage;
import com.company.framework.pages.LoginPage;
import com.company.framework.reporting.ExtentReport;

public class LoginToZomato extends BaseTest {

	@Test
	public void validLogin() {
		HomePage homePage = new HomePage();
		homePage.clickLoginButton();

		LoginPage loginPage = new LoginPage();
		loginPage.clickAndEnterEmail();

		Assert.assertTrue(homePage.isUserProfileAvatarDisplayed(),
				"User profile avatar should be displayed after successful login");
		ExtentReport.getTest().pass("LoginToZomato successfully");
	}
}



