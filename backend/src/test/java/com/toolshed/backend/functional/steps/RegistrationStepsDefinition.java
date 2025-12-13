package com.toolshed.backend.functional.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class RegistrationStepsDefinition {

    @Autowired
    private LoginStepsDefinition loginSteps; // Reuse browser context

    private Page getPage() {
        return loginSteps.page;
    }

    @Given("I am on the registration page")
    public void i_am_on_the_registration_page() {
        Page page = getPage();
        page.navigate("http://localhost:5173/register");
        assertTrue(page.url().contains("/register"));
    }

    @Given("a user with email {string} already exists")
    public void a_user_with_email_already_exists(String email) {
        // For functional tests, we assume the backend has this data
        // In real implementation, this might involve API calls to create test data
    }

    @When("I fill in the registration form with:")
    public void i_fill_in_the_registration_form_with(DataTable dataTable) {
        Page page = getPage();
        var data = dataTable.asMaps(String.class, String.class);
        
        for (var row : data) {
            // Handle the direct field-value mapping format from our feature files
            if (row.containsKey("firstName") && row.get("firstName") != null) {
                page.fill("[data-testid='first-name'], input[name='firstName'], #firstName", row.get("firstName"));
            }
            if (row.containsKey("lastName") && row.get("lastName") != null) {
                page.fill("[data-testid='last-name'], input[name='lastName'], #lastName", row.get("lastName"));
            }
            if (row.containsKey("email") && row.get("email") != null && !row.get("email").equals("[empty]")) {
                page.fill("[data-testid='email'], input[name='email'], input[type='email'], #email", row.get("email"));
            }
            if (row.containsKey("password") && row.get("password") != null && !row.get("password").equals("[empty]")) {
                page.fill("[data-testid='password'], input[name='password'], input[type='password'], #password", row.get("password"));
            }
            if (row.containsKey("role") && row.get("role") != null && !row.get("role").equals("[empty]")) {
                page.selectOption("[data-testid='role'], select[name='role'], #role", row.get("role"));
            }
        }
    }

    @When("I click the register button")
    public void i_click_the_register_button() {
        Page page = getPage();
        page.click("[data-testid='register-button'], button[type='submit'], .register-button, #register-btn, button:has-text('Register')");
    }

    @When("I enter email {string} in the email field")
    public void i_enter_email_in_the_email_field(String email) {
        Page page = getPage();
        page.fill("[data-testid='email'], input[name='email'], input[type='email'], #email", email);
    }

    @When("I trigger email validation")
    public void i_trigger_email_validation() {
        Page page = getPage();
        // Trigger validation by clicking outside or pressing tab
        page.press("[data-testid='email'], input[name='email'], input[type='email'], #email", "Tab");
    }

    @When("I start typing email {string}")
    public void i_start_typing_email(String email) {
        Page page = getPage();
        page.fill("[data-testid='email'], input[name='email'], input[type='email'], #email", "");
        // Type character by character to simulate real user typing
        for (char c : email.toCharArray()) {
            page.keyboard().type(String.valueOf(c));
        }
    }

    @When("I correct the email to {string}")
    public void i_correct_the_email_to(String email) {
        Page page = getPage();
        page.fill("[data-testid='email'], input[name='email'], input[type='email'], #email", "");
        page.fill("[data-testid='email'], input[name='email'], input[type='email'], #email", email);
    }

    @Then("I should remain on the registration page")
    public void i_should_remain_on_the_registration_page() {
        Page page = getPage();
        assertTrue(page.url().contains("/register"));
    }

    @Then("I should see a success message {string}")
    public void i_should_see_a_success_message(String message) {
        Page page = getPage();
        Locator successMessage = page.locator("[data-testid='success-message'], .success-message, .alert-success, .text-green-500");
        assertTrue(successMessage.isVisible());
        assertTrue(successMessage.textContent().contains(message));
    }

    @Then("I should be redirected to the login page")
    public void i_should_be_redirected_to_the_login_page() {
        Page page = getPage();
        page.waitForURL("**/login");
        assertTrue(page.url().contains("/login"));
    }

    @Then("I should see a message {string}")
    public void i_should_see_a_message(String message) {
        Page page = getPage();
        assertTrue(page.textContent("body").contains(message));
    }

    @Then("the email field should show a red warning")
    public void the_email_field_should_show_a_red_warning() {
        Page page = getPage();
        Locator emailField = page.locator("[data-testid='email'], input[name='email'], input[type='email'], #email").first();
        // Check for validation error styling
        String className = emailField.getAttribute("class");
        assertTrue((className != null && className.contains("error")) || 
                  page.locator("[data-testid='email-error'], .email-error, .error-message").isVisible());
    }

    @Then("the email field should show a green checkmark")
    public void the_email_field_should_show_a_green_checkmark() {
        Page page = getPage();
        Locator successIcon = page.locator("[data-testid='email-success'], .email-success, .success-icon, .check-icon");
        assertTrue(successIcon.isVisible());
    }

    @Then("the email field should show a validation error")
    public void the_email_field_should_show_a_validation_error() {
        Page page = getPage();
        Locator errorMessage = page.locator("[data-testid='email-error'], .email-error, .error-message, .validation-error");
        assertTrue(errorMessage.isVisible());
    }

    @Then("the validation should trigger automatically")
    public void the_validation_should_trigger_automatically() {
        Page page = getPage();
        // Wait for validation to appear
        page.waitForSelector("[data-testid='email-validation'], .email-validation, .validation-error, .error-message", new Page.WaitForSelectorOptions().setTimeout(2000));
    }

    @Then("I should see real-time validation feedback")
    public void i_should_see_real_time_validation_feedback() {
        Page page = getPage();
        // Check that validation feedback is present
        assertTrue(page.locator("[data-testid='email-validation'], .email-validation, .validation-error").isVisible() ||
                  page.locator("[data-testid='email-error'], .email-error, .error-message").isVisible() ||
                  page.locator("[data-testid='email-success'], .email-success, .success-icon").isVisible());
    }

    @Then("I should see appropriate validation feedback based on email length constraints")
    public void i_should_see_appropriate_validation_feedback_based_on_email_length_constraints() {
        Page page = getPage();
        // Check for length-based validation feedback
        assertTrue(page.locator("[data-testid='email-validation'], .email-validation, [data-testid='email-error'], .email-error, .error-message").isVisible());
    }
}