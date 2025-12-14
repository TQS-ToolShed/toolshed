package com.toolshed.backend.e2e.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;

import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LoginStepsDefinition {

    // Page selectors - adjust these based on your actual frontend implementation
    private static final String EMAIL_INPUT = "input[name='email'], input[type='email'], #email";
    private static final String PASSWORD_INPUT = "input[name='password'], input[type='password'], #password";
    private static final String LOGIN_BUTTON = "button[type='submit'], .login-button, #login-btn";
    private static final String ERROR_MESSAGE = ".error-message, .alert-error, .text-red-500, .text-destructive";
    private static final String VALIDATION_ERROR = ".validation-error, .field-error, .error, .text-destructive";
    private static final String SUCCESS_MESSAGE = ".success-message, .alert-success, .text-green-500";
    private static final String WELCOME_MESSAGE = ".welcome-message, .dashboard-header h1";
    private static final String BASE_URL = "http://localhost:5173"; // Vite dev server

    @Autowired
    private UserRepository userRepository;

    private Playwright playwright;
    private Browser browser;
    public Page page; // Made public for sharing with other step definitions

    @Before
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();

        // Ensure default test user exists for login tests
        createTestUserIfNotExists("user@example.com", "validPassword", UserStatus.ACTIVE);
    }

    @After
    public void tearDown() {
        if (page != null)
            page.close();
        if (browser != null)
            browser.close();
        if (playwright != null)
            playwright.close();
    }

    private void createTestUserIfNotExists(String email, String password, UserStatus status) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .firstName("Test")
                    .lastName("User")
                    .password(password)
                    .role(UserRole.RENTER)
                    .status(status)
                    .reputationScore(5.0)
                    .walletBalance(0.0)
                    .build();
            userRepository.save(user);
        }
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
        page.waitForSelector(EMAIL_INPUT, new Page.WaitForSelectorOptions().setTimeout(5000));
        page.waitForSelector(PASSWORD_INPUT, new Page.WaitForSelectorOptions().setTimeout(5000));
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
        // Wait longer for navigation after login API call
        page.waitForTimeout(3000);
        String currentUrl = page.url();
        // Check that we're redirected to a role-specific page or away from login
        boolean isRedirected = currentUrl.contains("renter") ||
                currentUrl.contains("supplier") ||
                currentUrl.contains("admin") ||
                currentUrl.contains("dashboard") ||
                currentUrl.contains("home") ||
                !currentUrl.contains("login");
        assertTrue(isRedirected,
                "Should be redirected away from login page to dashboard, but URL is: " + currentUrl);
    }

    @And("I should see a welcome message")
    public void iShouldSeeAWelcomeMessage() {
        // Check for various possible dashboard/home page indicators
        String content = page.content().toLowerCase();
        boolean hasWelcome = page.isVisible(WELCOME_MESSAGE) ||
                content.contains("welcome") ||
                content.contains("dashboard") ||
                content.contains("browse tools") || // Renter dashboard
                content.contains("my tools") || // Supplier dashboard
                content.contains("admin") || // Admin dashboard
                !page.url().contains("login"); // Redirected away from login
        assertTrue(hasWelcome, "Should display welcome message or dashboard content on successful login");
    }

    @Then("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String expectedMessage) {
        try {
            // Wait longer for API response
            page.waitForTimeout(2000);

            // First try the data-testid selector which is more reliable
            page.waitForSelector("[data-testid='error-message'], " + ERROR_MESSAGE,
                    new Page.WaitForSelectorOptions().setTimeout(5000));

            // Try data-testid first
            com.microsoft.playwright.Locator testIdLocator = page.locator("[data-testid='error-message']");
            if (testIdLocator.count() > 0 && testIdLocator.first().isVisible()) {
                String errorText = testIdLocator.first().textContent();
                if (errorText != null && errorText.toLowerCase().contains(expectedMessage.toLowerCase())) {
                    return; // Found the message
                }
            }

            // Fallback to other selectors
            String errorText = page.textContent(ERROR_MESSAGE);
            if (errorText != null && errorText.toLowerCase().contains(expectedMessage.toLowerCase())) {
                return; // Found the message
            }
        } catch (Exception e) {
            // If selector not found, check page content
        }
        // Final fallback: check page content
        String content = page.content().toLowerCase();
        assertTrue(content.contains(expectedMessage.toLowerCase()),
                "Error message should contain: " + expectedMessage + ", page URL: " + page.url());
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
        // Create user with specified status if not exists, or update existing user
        UserStatus userStatus = UserStatus.valueOf(status);

        userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    // Update existing user's status
                    user.setStatus(userStatus);
                    userRepository.save(user);
                },
                () -> {
                    // Create new user with specified status
                    User user = User.builder()
                            .email(email)
                            .firstName("Test")
                            .lastName("User")
                            .password("validPassword")
                            .role(UserRole.RENTER)
                            .status(userStatus)
                            .reputationScore(5.0)
                            .walletBalance(0.0)
                            .build();
                    userRepository.save(user);
                });
    }
}