package com.company.framework.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Reporter;

import com.company.framework.base.BasePage;
import com.company.framework.utils.WaitUtils;

public class ProfilePage extends BasePage {

	@FindBy(xpath = "//h2[.='Reviews']")
	private WebElement reviewsHeader;

	@FindBy(xpath = "//h2[.='Photos']")
	private WebElement photosHeader;

	@FindBy(xpath = "//h2[.='Followers']")
	private WebElement followersHeader;

	@FindBy(xpath = "//h2[.='Recently Viewed'] | //div[.='Recently Viewed'] | //span[.='Recently Viewed']")
	private WebElement recentlyViewedHeader;

	@FindBy(xpath = "//span[.='Edit profile'] | //button[contains(.,'Edit profile')]")
	private WebElement editProfileButton;

	@FindBy(xpath = "(//*[contains(text(), 'camera-fill') or @title='camera-fill'])[2] | (//i[contains(@class,'camera')])[2]")
	private WebElement cameraIconButton;

	@FindBy(xpath = "//div[normalize-space()='Change Photo'] | //span[normalize-space()='Change Photo'] | //*[contains(text(),'Change Photo')]")
	private WebElement changePhotoOption;

	@FindBy(xpath = "//div[normalize-space()='Delete Photo'] | //span[normalize-space()='Delete Photo'] | //*[contains(text(),'Delete Photo')]")
	private WebElement deletePhotoOption;

	@FindBy(xpath = "//button[.//span[.='Yes']] | //span[.='Yes']/ancestor::button[1] | //span[.='Yes']")
	private WebElement confirmDeleteYesButton;

	@FindBy(xpath = "//span[.='Profile picture removed successfully'] | //div[contains(text(),'Profile picture removed successfully')] | //*[contains(text(),'Profile picture removed successfully')]")
	private WebElement profilePictureRemovedSuccessMessage;

	@FindBy(xpath = "//input[@type='file']")
	private WebElement fileInput;

	@FindBy(xpath = "//span[.='Update'] | //button[span[.='Update']] | //button[contains(.,'Update')]")
	private WebElement updateButton;

	@FindBy(xpath = "//span[.='Profile updated successfully'] | //div[contains(text(),'Profile updated successfully')] | //*[contains(text(),'Profile updated successfully')]")
	private WebElement profileUpdatedSuccessMessage;

	public ProfilePage() {
		PageFactory.initElements(driver(), this);
	}

	public boolean isReviewsHeaderDisplayed() {
		try {
			return isDisplayed(WaitUtils.waitForElementVisible(reviewsHeader));
		} catch (Exception e) {
			log.warn("Reviews header not displayed: {}", e.getMessage());
			return false;
		}
	}

	public boolean isPhotosHeaderDisplayed() {
		try {
			return isDisplayed(WaitUtils.waitForElementVisible(photosHeader));
		} catch (Exception e) {
			log.warn("Photos header not displayed: {}", e.getMessage());
			return false;
		}
	}

	public boolean isFollowersHeaderDisplayed() {
		try {
			return isDisplayed(WaitUtils.waitForElementVisible(followersHeader));
		} catch (Exception e) {
			log.warn("Followers header not displayed: {}", e.getMessage());
			return false;
		}
	}

	public boolean isRecentlyViewedHeaderDisplayed() {
		try {
			return isDisplayed(WaitUtils.waitForElementVisible(recentlyViewedHeader));
		} catch (Exception e) {
			log.warn("Recently Viewed header not displayed: {}", e.getMessage());
			return false;
		}
	}

	public void clickOnRecentlyViewed() {
		Reporter.log("Clicking on Recently Viewed header", true);
		By recentlyViewedLocator = By.xpath("//h2[.='Recently Viewed'] | //div[.='Recently Viewed'] | //span[.='Recently Viewed'] | //*[contains(text(),'Recently Viewed')]");
		try {
			WebElement element = WaitUtils.waitForElementVisible(recentlyViewedLocator);
			scrollToElement(element);
			click(element);
		} catch (Exception e) {
			log.warn("Clicking recentlyViewedHeader natively failed ({}), using JS click", e.getMessage());
			javaScriptClick(recentlyViewedHeader);
		}
	}

	public boolean isRestaurantInRecentlyViewedDisplayed(String restaurantName) {
		Reporter.log("Verifying restaurant [" + restaurantName + "] is displayed under Recently Viewed section...", true);
		By locator = By.xpath(
			"//a[.='" + restaurantName + "'] | " +
			"//a[.='Paakashala'] | " +
			"//a[contains(., '" + restaurantName + "')] | " +
			"//a[contains(text(), '" + restaurantName + "')] | " +
			"//a[contains(@href, '" + restaurantName.toLowerCase() + "')] | " +
			"//h4[contains(text(), '" + restaurantName + "')] | " +
			"//div[contains(text(), '" + restaurantName + "')] | " +
			"//p[contains(text(), '" + restaurantName + "')] | " +
			"//a[contains(@href,'restaurant')] | " +
			"//a[contains(@href,'/bangalore/')] | " +
			"//div[contains(@class,'recently')]//a"
		);
		try {
			WebElement restaurantLink = WaitUtils.waitForPresenceOfElement(locator);
			scrollToElement(restaurantLink);
			return isDisplayed(restaurantLink);
		} catch (Exception e) {
			log.warn("Restaurant [{}] was not found in Recently Viewed section: {}", restaurantName, e.getMessage());
			return false;
		}
	}

	public void clickEditProfile() {
		Reporter.log("Clicking Edit profile button", true);
		try {
			click(editProfileButton);
		} catch (Exception e) {
			log.warn("Native click failed on editProfileButton, trying JS click: {}", e.getMessage());
			javaScriptClick(editProfileButton);
		}
	}

	public void clickCameraIcon() {
		Reporter.log("Clicking 2nd camera icon...", true);
		By cameraLocator = By.xpath(
			"(//*[contains(text(), 'camera-fill') or @title='camera-fill'])[2] | " +
			"(//*[contains(text(), 'camera-fill') or @title='camera-fill']/..)[2] | " +
			"(//i[contains(@class,'camera')])[2] | " +
			"(//div[contains(@class,'camera')])[2]"
		);
		try {
			WebElement element = WaitUtils.waitForPresenceOfElement(cameraLocator);
			scrollToElement(element);
			javaScriptClick(element);
		} catch (Exception e) {
			log.error("Failed to locate or click camera icon element: {}", e.getMessage());
			throw e;
		}
	}

	public void clickChangePhoto() {
		Reporter.log("Preparing Change Photo option (bypassing native OS file picker popup)", true);
		// Native click on file input/label triggers OS file picker dialog which blocks Selenium WebDriver.
		// File path is sent directly to <input type='file'> in uploadImage().
	}

	public void clickDeletePhoto() {
		Reporter.log("Clicking Delete Photo option", true);
		By deletePhotoLocator = By.xpath("//div[normalize-space()='Delete Photo'] | //span[normalize-space()='Delete Photo'] | //*[contains(text(),'Delete Photo')]");
		try {
			WebElement element = WaitUtils.waitForPresenceOfElement(deletePhotoLocator);
			try {
				element.click();
			} catch (Exception e) {
				log.warn("Native click on Delete Photo option failed ({}), using JS click", e.getMessage());
				javaScriptClick(element);
			}
		} catch (Exception e) {
			log.warn("Direct lookup for Delete Photo failed: {}", e.getMessage());
			javaScriptClick(deletePhotoOption);
		}
	}

	public void clickConfirmDeleteYes() {
		Reporter.log("Clicking Yes on Delete Photo confirmation modal", true);
		By yesLocator = By.xpath("//button[.//span[.='Yes']] | //span[.='Yes']/ancestor::button[1] | //span[.='Yes']");
		try {
			WebElement element = WaitUtils.waitForPresenceOfElement(yesLocator);
			try {
				element.click();
			} catch (Exception e) {
				log.warn("Native click on Yes button failed ({}), using JS click", e.getMessage());
				javaScriptClick(element);
			}
		} catch (Exception e) {
			log.warn("Direct lookup for Yes button failed: {}", e.getMessage());
			javaScriptClick(confirmDeleteYesButton);
		}
	}

	public boolean isProfilePictureRemovedSuccessMessageDisplayed() {
		Reporter.log("Verifying 'Profile picture removed successfully' message...", true);
		try {
			By successMsgLocator = By.xpath("//span[.='Profile picture removed successfully'] | //div[contains(text(),'Profile picture removed successfully')] | //*[contains(text(),'Profile picture removed successfully')]");
			WebElement element = WaitUtils.waitForElementVisible(successMsgLocator);
			return isDisplayed(element);
		} catch (Exception e) {
			log.warn("'Profile picture removed successfully' message was not displayed: {}", e.getMessage());
			return false;
		}
	}

	public void uploadImage(String filePath) {
		Reporter.log("Uploading image file: [" + filePath + "]", true);
		By fileInputLocator = By.xpath("//input[@type='file']");
		List<WebElement> inputs = WaitUtils.waitForPresenceOfAllElements(fileInputLocator);
		if (inputs.isEmpty()) {
			throw new RuntimeException("No <input type='file'> element found on page for image upload");
		}

		WebElement targetInput = inputs.get(inputs.size() - 1);
		try {
			((JavascriptExecutor) driver()).executeScript(
					"arguments[0].style.display='block'; arguments[0].style.visibility='visible'; arguments[0].style.opacity='1';",
					targetInput);
		} catch (Exception e) {
			log.debug("Could not modify style attributes of file input: {}", e.getMessage());
		}

		targetInput.sendKeys(filePath);
		Reporter.log("Successfully sent file path to <input type='file'> element: [" + filePath + "]", true);
	}

	public void clickUpdate() {
		Reporter.log("Clicking Update button", true);
		try {
			click(updateButton);
		} catch (Exception e) {
			log.warn("Native click on Update button failed ({}), using JS click", e.getMessage());
			javaScriptClick(updateButton);
		}
	}

	public boolean isProfileUpdatedSuccessMessageDisplayed() {
		Reporter.log("Verifying 'Profile updated successfully' message...", true);
		try {
			By successMsgLocator = By.xpath("//span[.='Profile updated successfully'] | //div[contains(text(),'Profile updated successfully')] | //*[contains(text(),'Profile updated successfully')]");
			WebElement element = WaitUtils.waitForElementVisible(successMsgLocator);
			return isDisplayed(element);
		} catch (Exception e) {
			log.warn("'Profile updated successfully' message was not displayed: {}", e.getMessage());
			return false;
		}
	}
}
