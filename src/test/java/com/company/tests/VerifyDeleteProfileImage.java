package com.company.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.company.framework.base.BaseTest;
import com.company.framework.pages.HomePage;
import com.company.framework.pages.LoginPage;
import com.company.framework.pages.ProfilePage;
import com.company.framework.reporting.ExtentReport;

public class VerifyDeleteProfileImage extends BaseTest {

	@Test
	public void verifyUserIsAbleToDeleteProfileImage() {
		HomePage homePage = new HomePage();
		homePage.clickLoginButton();

		LoginPage loginPage = new LoginPage();
		loginPage.clickAndEnterEmail();

		homePage.clickUserProfileAvatar();
		homePage.clickProfileOption();

		ProfilePage profilePage = new ProfilePage();
		profilePage.clickEditProfile();
		profilePage.clickCameraIcon();
		profilePage.clickDeletePhoto();
		profilePage.clickConfirmDeleteYes();

		Assert.assertTrue(profilePage.isProfilePictureRemovedSuccessMessageDisplayed(),
				"'Profile picture removed successfully' message should be displayed after deleting profile picture");
		//ExtentReport.getTest().pass("VerifyDeleteProfileImage successfully");
	}
}
