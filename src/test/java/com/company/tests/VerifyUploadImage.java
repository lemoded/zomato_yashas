package com.company.tests;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.company.framework.base.BaseTest;
import com.company.framework.pages.HomePage;
import com.company.framework.pages.LoginPage;
import com.company.framework.pages.ProfilePage;

public class VerifyUploadImage extends BaseTest {

	@Test
	public void verifyUserIsAbleToUploadImage() {
		HomePage homePage = new HomePage();
		homePage.clickLoginButton();

		LoginPage loginPage = new LoginPage();
		loginPage.clickAndEnterEmail();

		homePage.clickUserProfileAvatar();
		homePage.clickProfileOption();

		ProfilePage profilePage = new ProfilePage();
		profilePage.clickEditProfile();
		profilePage.clickCameraIcon();
		profilePage.clickChangePhoto();

		File customImage = new File("C:/Users/Yashasa/Downloads/Screenshot 2026-08-11 114142.jpg");
		File fallbackImage = new File("src/test/resources/sample_image.jpg");

		File fileToUpload = customImage.exists() ? customImage : fallbackImage;
		profilePage.uploadImage(fileToUpload.getAbsolutePath());

		profilePage.clickUpdate();
		Assert.assertTrue(profilePage.isProfileUpdatedSuccessMessageDisplayed(),
				"'Profile updated successfully' message should be displayed after updating profile");
	}
}
