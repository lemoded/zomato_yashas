package com.company.tests;

import org.testng.annotations.Test;

import com.company.framework.base.BaseTest;
import com.company.framework.pages.HomePage;
import com.company.framework.pages.LoginPage;

public class LoginToZomato extends BaseTest {

	@Test
	public void validLogin() {
		HomePage homePage = new HomePage();
		homePage.clickLoginButton();

		LoginPage loginPage = new LoginPage();
		loginPage.clickAndEnterEmail();




	}
}



