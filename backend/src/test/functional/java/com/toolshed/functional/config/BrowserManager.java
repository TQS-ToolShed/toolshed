package com.toolshed.functional.config;

import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;

@Component
public class BrowserManager {
    
    private static Playwright playwright;
    private static Browser browser;
    private static BrowserContext context;
    private static Page page;
    
    // Configuration
    private static final String BASE_URL = "http://localhost:5173"; // Vite dev server default port
    private static final boolean HEADLESS = true; // Set to false for debugging
    private static final double TIMEOUT = 30000; // 30 seconds
    
    public static void initializeBrowser() {
        if (playwright == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(HEADLESS)
                .setSlowMo(50)); // Add slight delay for stability
            
            context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720)
                .setIgnoreHTTPSErrors(true));
            
            // Set default timeout
            context.setDefaultTimeout(TIMEOUT);
            
            page = context.newPage();
        }
    }
    
    public static Page getPage() {
        if (page == null) {
            initializeBrowser();
        }
        return page;
    }
    
    public static void navigateToUrl(String path) {
        getPage().navigate(BASE_URL + path);
        getPage().waitForLoadState(LoadState.NETWORKIDLE);
    }
    
    public static void navigateToLoginPage() {
        navigateToUrl("/login");
    }
    
    public static void navigateToRegisterPage() {
        navigateToUrl("/register");
    }
    
    public static void navigateToHomePage() {
        navigateToUrl("/");
    }
    
    public static void closeBrowser() {
        if (page != null) {
            page.close();
            page = null;
        }
        if (context != null) {
            context.close();
            context = null;
        }
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }
    
    public static void takeScreenshot(String name) {
        if (page != null) {
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("target/screenshots/" + name + ".png"))
                .setFullPage(true));
        }
    }
    
    // Utility methods for common actions
    public static void fillField(String selector, String value) {
        getPage().fill(selector, value);
    }
    
    public static void clickElement(String selector) {
        getPage().click(selector);
    }
    
    public static String getElementText(String selector) {
        return getPage().textContent(selector);
    }
    
    public static boolean isElementVisible(String selector) {
        return getPage().isVisible(selector);
    }
    
    public static void waitForElement(String selector) {
        getPage().waitForSelector(selector);
    }
    
    public static String getCurrentUrl() {
        return getPage().url();
    }
}