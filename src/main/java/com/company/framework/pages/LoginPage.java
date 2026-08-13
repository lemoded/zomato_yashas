package com.company.framework.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Reporter;

import com.company.framework.base.BasePage;
import com.company.framework.utils.EmailUtils;
import com.company.framework.utils.WaitUtils;

public class LoginPage extends BasePage {

	private static final String DEFAULT_EMAIL = "contact.yashasy@gmail.com";
	private static final String DEFAULT_APP_PASSWORD = "meod tual erdd aiqo";

	@FindBy(xpath = "//span[.=\"Continue with Email\"] | //span[contains(text(),'Continue with Email')] | //div[contains(text(),'Continue with Email')]")
	private WebElement emailbutton;

	@FindBy(xpath = "//label[.=\"Email\"]/..//input | //label[contains(text(),'Email')]/..//input | //input[@type='email']")
	private WebElement emailTextField;

	@FindBy(xpath = "//span[.=\"Send One Time Password\"]/.. | //span[contains(text(),'Send One Time Password')]/.. | //button[contains(.,'One Time Password')]")
	private WebElement oneTimePasswordButton;

	public LoginPage() {
		PageFactory.initElements(driver(), this);
	}

	public void clickAndEnterEmail() {
		clickAndEnterEmail(DEFAULT_EMAIL, DEFAULT_APP_PASSWORD);
	}

	public void clickAndEnterEmail(String emailStr) {
		clickAndEnterEmail(emailStr, DEFAULT_APP_PASSWORD);
	}

	public void clickAndEnterEmail(String emailStr, String appPassword) {
		switchToAuthIframe();
		try {
			click(emailbutton);
			sendKeys(emailTextField, emailStr);
			click(oneTimePasswordButton);

			Reporter.log("Fetching OTP from Gmail via IMAP...", true);
			String otpString = EmailUtils.fetchOtpFromEmail(emailStr, appPassword, 35);

			if (otpString == null || otpString.trim().isEmpty()) {
				log.warn("Automatic email OTP fetch failed or timed out. Falling back to manual scanner input...");
				System.out.print("Enter OTP received: ");
			}

			Reporter.log("Entering OTP code: [" + otpString + "]", true);
			By otpLocator = By.cssSelector("input.sc-hp56s6-1, input[type='number'], section input");
			List<WebElement> otpInputs = WaitUtils.waitForPresenceOfAllElements(otpLocator);
			for (int i = 0; i < otpString.length() && i < otpInputs.size(); i++) {
				WebElement currentInput = otpInputs.get(i);
				currentInput.clear();
				currentInput.sendKeys(String.valueOf(otpString.charAt(i)));
			}
		} finally {
			restoreDefaultContent();
		}
	}

	private void switchToAuthIframe() {
		By iframeLocator = By.cssSelector("iframe#auth-login-ui, iframe[src*='accounts.zomato.com'], iframe[id*='auth']");
		WebElement iframe = WaitUtils.waitForPresenceOfElement(iframeLocator);
		driver().switchTo().frame(iframe);
		Reporter.log("Switched to authentication iframe: [" + iframeLocator + "]", true);
	}

	private void restoreDefaultContent() {
		try {
			driver().switchTo().defaultContent();
			Reporter.log("Switched back to default content", true);
			By iframeLocator = By.cssSelector("iframe#auth-login-ui, iframe[src*='accounts.zomato.com'], iframe[id*='auth']");
			try {
				WaitUtils.waitForInvisibilityOfElement(iframeLocator);
				Reporter.log("Auth iframe is now closed/invisible", true);
			} catch (Exception e) {
				log.debug("Iframe invisibility wait completed/timed out: {}", e.getMessage());
			}
		} catch (Exception e) {
			log.warn("Failed to switch back to default content: {}", e.getMessage());
		}
	}
}
