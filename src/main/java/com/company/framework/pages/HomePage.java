package com.company.framework.pages;

import com.company.framework.base.BasePage;
import com.company.framework.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

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
    
    @FindBy(xpath = "//input[@placeholder=\"Search for restaurant, cuisine or a dish\"]/following-sibling::div/div[1]")
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

	public void clickProfileOption() {
		click(profileOption);
	}
	
	public String enterDataInSearchTF()
	{
		click(searchElement);
		log.info("Click on search textfield");
		sendKeys(searchElement, searchData);
		log.info("Entering " + searchData);
		click(firstSuggestedFieldElement);
		log.info("click on the first resto");
		String firstRestaurant = getText(firstRestaurantElement);
		log.info("Name of the first resto is " + firstRestaurant);
		click(firstRestaurantElement);
		return firstRestaurant;
		
	}
}
