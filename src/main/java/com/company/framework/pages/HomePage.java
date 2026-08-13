package com.company.framework.pages;

import com.company.framework.base.BasePage;
import com.company.framework.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Reporter;

public class HomePage extends BasePage {
	
	private String searchData = "panner";

    @FindBy(xpath = "//input[contains(@placeholder,\"Search for\")]")
    private WebElement searchBox;
    
    
    @FindBy(xpath = "//button[.='Log in']")
    private WebElement loginButton;

    @FindBy(xpath = "//div[div[contains(@src, 'b.zmtcdn.com/web/assets')]] | //div[img[contains(@src, 'b.zmtcdn.com/web/assets')]] | //span[.=\"Yashas\"]")
    private WebElement userProfileAvatar;

    @FindBy(xpath = "//div[.='Profile']")
    private WebElement profileOption;
    
    @FindBy(xpath = "//input[@placeholder=\"Search for restaurant, cuisine or a dish\"]")
    private WebElement searchElement;
    
    @FindBy(xpath = "//input[@placeholder='Search for restaurant, cuisine or a dish']/following-sibling::div/div[1]")
    private WebElement firstSuggestedFieldElement;
    
    @FindBy(xpath = "(//h4)[1]")
    private WebElement firstRestaurantElement;


    public HomePage() {
        PageFactory.initElements(driver(), this);
    }

 
	public void clickSearchBox() {
	    searchBox.click();
		
	}
	public void clickLoginButton()
	{
		loginButton.click();
	}

	public void clickUserProfileAvatar() {
		click(userProfileAvatar);
	}

	public boolean isUserProfileAvatarDisplayed() {
		try {
			return isDisplayed(WaitUtils.waitForElementVisible(userProfileAvatar));
		} catch (Exception e) {
			log.warn("User profile avatar is not displayed: {}", e.getMessage());
			return false;
		}
	}

	public void clickProfileOption() {
		click(profileOption);
	}
	
	public String enterDataInSearchTF()
	{
		click(searchElement);
		Reporter.log("Click on search textfield", true);
		sendKeys(searchElement, searchData);
		Reporter.log("Entering " + searchData, true);
		click(firstSuggestedFieldElement);
		Reporter.log("click on the first resto", true);
		String firstRestaurant = getText(firstRestaurantElement);
		Reporter.log("Name of the first resto is " + firstRestaurant, true);
		click(firstRestaurantElement);
		return firstRestaurant;
		
	}
}
