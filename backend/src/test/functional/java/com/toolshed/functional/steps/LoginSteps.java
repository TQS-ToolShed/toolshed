package com.toolshed.functional.steps;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class LoginSteps {

    // Page selectors - adjust these based on your actual frontend implementation
    private static final String EMAIL_INPUT = "input[name='email'], input[type='email'], #email";
    private static final String PASSWORD_INPUT = "input[name='password'], input[type='password'], #password";
    private static final String LOGIN_BUTTON = "button[type='submit'], .login-button, #login-btn";
    private static final String ERROR_MESSAGE = ".error-message, .alert-error, .text-red-500";
    private static final String VALIDATION_ERROR = ".validation-error, .field-error, .error";
    private static final String SUCCESS_MESSAGE = ".success-message, .alert-success, .text-green-500";
    private static final String WELCOME_MESSAGE = ".welcome-message, .dashboard-header h1";
    private static final String BASE_URL = "http://localhost:5173"; // Vite dev server

    private static Playwright playwright;
    private static Browser browser;
    public static Page page;

    @Before("@login")
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
    }

    @After("@login")
    public void tearDown() {
        if (page != null)
            page.close();
        if (browser != null)
            browser.close();
        if (playwright != null)
            playwright.close();
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

    @And("I am on the login page")
    public void iAmOnTheLoginPage() {
        page.navigate(BASE_URL + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        // Wait for login form to be visible
        page.waitForSelector(EMAIL_INPUT);
        page.waitForSelector(PASSWORD_INPUT);
        assertTrue(page.url().contains("login"),
                "Should be on login page");
    }

    @When("I enter email {string} and password {string}")
    public void iEnterEmailAndPassword(String email, String password) {
        page.fill(EMAIL_INPUT, email);
        page.fill(PASSWORD_INPUT, password);
    }

    @And("I click the login button")
    public void iClickTheLoginButton() {
        page.click(LOGIN_BUTTON);
        // Wait a moment for the response
        page.waitForTimeout(1000);
    }

    @Then("I should be redirected to the dashboard")
    public void iShouldBeRedirectedToTheDashboard() {
        // Wait for navigation
        page.waitForTimeout(2000);
        String currentUrl = page.url();
        assertTrue(currentUrl.contains("dashboard") || currentUrl.contains("home") ||
                !currentUrl.contains("login"),
                "Should be redirected away from login page to dashboard");
    }

    @And("I should see a welcome message")
    public void iShouldSeeAWelcomeMessage() {
        // Check for various possible welcome message formats
        boolean hasWelcome = page.isVisible(WELCOME_MESSAGE) ||
                page.content().toLowerCase().contains("welcome") ||
                page.content().toLowerCase().contains("dashboard");
        assertTrue(hasWelcome, "Should display welcome message on successful login");
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

    @And("I should remain on the login page")
    public void iShouldRemainOnTheLoginPage() {
        assertTrue(page.url().contains("login"),
                "Should remain on login page after error");
    }

    @Then("I should see a validation error {string}")
    public void iShouldSeeAValidationError(String expectedError) {
        // Check for validation error in various possible locations
        boolean hasValidationError = page.isVisible(VALIDATION_ERROR) ||
                page.content().toLowerCase().contains(expectedError.toLowerCase());
        assertTrue(hasValidationError, "Should display validation error: " + expectedError);
    }

    @Given("a user with email {string} has status {string}")
    public void aUserWithEmailHasStatus(String email, String status) {
        // This would typically involve setting up test data in the database
        // For functional tests, we assume this data exists or we set it up through API
        // calls
        // Implementation depends on your backend setup and test data management
        // strategy

        // Example API call to create test user (adjust based on your API):
        // RestAssured.given()
        // .contentType(ContentType.JSON)
        // .body(new CreateUserRequest(email, "password", status))
        // .post("/api/test/users")
        // .then()
        // .statusCode(201);

        // For now, we'll assume the test data is managed externally
        System.out.println("Test setup: User " + email + " should have status " + status);
    }
}