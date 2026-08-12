package com.company.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.company.framework.base.BaseTest;
import com.company.framework.pages.HomePage;
import com.company.framework.pages.LoginPage;
import com.company.framework.pages.ProfilePage;

public class VerifyTheProfilefunctionality extends BaseTest {

	@Test
	public void verifyProfileFunctionality() {
		HomePage homePage = new HomePage();
		homePage.clickLoginButton();

		LoginPage loginPage = new LoginPage();
		loginPage.clickAndEnterEmail();

		homePage.clickUserProfileAvatar();
		homePage.clickProfileOption();

		ProfilePage profilePage = new ProfilePage();
		Assert.assertTrue(profilePage.isReviewsHeaderDisplayed(), "Reviews section heading should be visible on Profile page");
		Assert.assertTrue(profilePage.isPhotosHeaderDisplayed(), "Photos section heading should be visible on Profile page");
		Assert.assertTrue(profilePage.isFollowersHeaderDisplayed(), "Followers section heading should be visible on Profile page");
		Assert.assertTrue(profilePage.isRecentlyViewedHeaderDisplayed(), "Recently Viewed section heading should be visible on Profile page");
	}
}
