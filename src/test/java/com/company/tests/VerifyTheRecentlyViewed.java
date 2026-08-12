package com.company.tests;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.company.framework.base.BaseTest;
import com.company.framework.driver.DriverManager;
import com.company.framework.pages.HomePage;
import com.company.framework.pages.LoginPage;
import com.company.framework.pages.ProfilePage;
import com.company.framework.utils.PropertyUtils;

public class VerifyTheRecentlyViewed extends BaseTest {

	@Test
	public void verifyTheRecentlyViewed() {
		HomePage homePage = new HomePage();
		homePage.clickLoginButton();
		LoginPage loginPage = new LoginPage();
		loginPage.clickAndEnterEmail();
		String firstRest = homePage.enterDataInSearchTF();
		homePage.clickUserProfileAvatar();
		homePage.clickProfileOption();
		ProfilePage profilePage = new ProfilePage();
		profilePage.clickOnRecentlyViewed();
		boolean isDisplayed = profilePage.isRestaurantInRecentlyViewedDisplayed(firstRest);
		Assert.assertTrue(isDisplayed,
				"Recently viewed restaurant " + firstRest + " should be displayed under Recently Viewed section on Profile page");

	}
}
