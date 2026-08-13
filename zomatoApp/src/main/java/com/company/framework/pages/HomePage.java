package com.company.framework.pages;

import com.company.framework.base.BasePage;
import com.company.framework.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class HomePage extends BasePage {

    @FindBy(xpath = "//input[contains(@placeholder,\"Search for\")]")
    private WebElement searchBox;


    public HomePage() {
        PageFactory.initElements(driver(), this);
    }

 
	public void clickSearchBox() {
	    searchBox.click();
		
	}
 

  
}
