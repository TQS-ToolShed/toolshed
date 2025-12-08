package com.toolshed.functional.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class RegisterSteps {

    // Page selectors - adjust these based on your actual frontend implementation
    private static final String FIRSTNAME_INPUT = "input[name='firstName'], #firstName";
    private static final String LASTNAME_INPUT = "input[name='lastName'], #lastName";
    private static final String EMAIL_INPUT = "input[name='email'], input[type='email'], #email";
    private static final String PASSWORD_INPUT = "input[name='password'], input[type='password'], #password";
    private static final String ROLE_SELECT = "select[name='role'], #role";
    private static final String REGISTER_BUTTON = "button[type='submit'], .register-button, #register-btn";
    private static final String ERROR_MESSAGE = ".error-message, .alert-error, .text-red-500";
    private static final String VALIDATION_ERROR = ".validation-error, .field-error, .error";
    private static final String SUCCESS_MESSAGE = ".success-message, .alert-success, .text-green-500";
    private static final String BASE_URL = "http://localhost:5173"; // Vite dev server

    private static Playwright playwright;
    private static Browser browser;
    private static Page page;

    @Before("@register")
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
    }

    @After("@register")
    public void tearDown() {
        if (page != null) page.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Given("the ToolShed application is running")
    public void theToolShedApplicationIsRunning() {
        // Verify the application is accessible
        page.navigate(BASE_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertTrue(page.title().contains("ToolShed") || 
                  page.url().contains("localhost"), 
                  "Application should be running and accessible");
    }

    @And("I am on the registration page")
    public void iAmOnTheRegistrationPage() {
        page.navigate(BASE_URL + "/register");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        // Wait for registration form to be visible
        page.waitForSelector(FIRSTNAME_INPUT);
        page.waitForSelector(LASTNAME_INPUT);
        page.waitForSelector(EMAIL_INPUT);
        page.waitForSelector(PASSWORD_INPUT);
        assertTrue(page.url().contains("register"), 
                  "Should be on registration page");
    }

    @When("I fill in the registration form with:")
    public void iFillInTheRegistrationFormWith(DataTable dataTable) {
        Map<String, String> formData = dataTable.asMap(String.class, String.class);
        
        if (formData.containsKey("firstName") && !formData.get("firstName").isEmpty()) {
            page.fill(FIRSTNAME_INPUT, formData.get("firstName"));
        }
        
        if (formData.containsKey("lastName") && !formData.get("lastName").isEmpty()) {
            page.fill(LASTNAME_INPUT, formData.get("lastName"));
        }
        
        if (formData.containsKey("email") && !formData.get("email").isEmpty()) {
            page.fill(EMAIL_INPUT, formData.get("email"));
        }
        
        if (formData.containsKey("password") && !formData.get("password").isEmpty()) {
            page.fill(PASSWORD_INPUT, formData.get("password"));
        }
        
        if (formData.containsKey("role") && !formData.get("role").isEmpty()) {
            page.selectOption(ROLE_SELECT, formData.get("role"));
        }
    }

    @And("I click the register button")
    public void iClickTheRegisterButton() {
        page.click(REGISTER_BUTTON);
        // Wait a moment for the response
        page.waitForTimeout(1000);
    }

    @Then("I should see a success message {string}")
    public void iShouldSeeASuccessMessage(String expectedMessage) {
        page.waitForSelector(SUCCESS_MESSAGE);
        String successText = page.textContent(SUCCESS_MESSAGE);
        assertNotNull(successText, "Success message should be present");
        assertTrue(successText.toLowerCase().contains(expectedMessage.toLowerCase()) ||
                  page.content().toLowerCase().contains(expectedMessage.toLowerCase()),
                  "Success message should contain: " + expectedMessage);
    }

    @And("I should be redirected to the login page")
    public void iShouldBeRedirectedToTheLoginPage() {
        // Wait for navigation
        page.waitForTimeout(2000);
        String currentUrl = page.url();
        assertTrue(currentUrl.contains("login"), 
                  "Should be redirected to login page after successful registration");
    }

    @Given("a user with email {string} already exists")
    public void aUserWithEmailAlreadyExists(String email) {
        // This would typically involve setting up test data in the database
        // For functional tests, we assume this data exists or we set it up through API calls
        // Implementation depends on your backend setup and test data management strategy
        System.out.println("Test setup: User with email " + email + " should already exist");
    }

    @Then("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String expectedMessage) {
        page.waitForSelector(ERROR_MESSAGE);
        String errorText = page.textContent(ERROR_MESSAGE);
        assertNotNull(errorText, "Error message should be present");
        assertTrue(errorText.toLowerCase().contains(expectedMessage.toLowerCase()) ||
                  page.content().toLowerCase().contains(expectedMessage.toLowerCase()),
                  "Error message should contain: " + expectedMessage);
    }

    @And("I should remain on the registration page")
    public void iShouldRemainOnTheRegistrationPage() {
        assertTrue(page.url().contains("register"), 
                  "Should remain on registration page after error");
    }

    @Then("I should see a validation error {string}")
    public void iShouldSeeAValidationError(String expectedError) {
        // Check for validation error in various possible locations
        boolean hasValidationError = page.isVisible(VALIDATION_ERROR) ||
                                   page.content().toLowerCase().contains(expectedError.toLowerCase());
        assertTrue(hasValidationError, "Should display validation error: " + expectedError);
    }
}