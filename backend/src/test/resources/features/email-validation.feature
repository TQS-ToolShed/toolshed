@e2e
Feature: Email Validation
  As a user filling out a registration form
  I want to check if my email is already registered
  So that I can know if I need to use a different email address

  Background:
    Given the ToolShed application is running
    And I am on the registration page

  Scenario: Check available email address
    When I enter email "newemail@example.com" in the email field
    And I trigger email validation
    Then I should see a message "Email is available"
    And the email field should show a green checkmark

  Scenario: Check already registered email address
    Given a user with email "registered@example.com" already exists
    When I enter email "registered@example.com" in the email field
    And I trigger email validation
    Then I should see a message "Email is already registered"
    And the email field should show a red warning

  Scenario: Check email with invalid format
    When I enter email "invalid-email" in the email field
    And I trigger email validation
    Then I should see a message "Please enter a valid email address"
    And the email field should show a validation error

  Scenario: Check empty email field
    When I enter email "" in the email field
    And I trigger email validation
    Then I should see a message "Email is required"
    And the email field should show a validation error

  Scenario: Real-time email validation during typing
    When I start typing email "existing@example.com"
    Then the validation should trigger automatically
    And I should see real-time validation feedback

  Scenario: Email validation with special characters
    When I enter email "user+tag@sub-domain.example.com" in the email field
    And I trigger email validation
    Then I should see a message "Email is available"
    And the email field should show a green checkmark

  Scenario: Email validation with very long email
    When I enter email "verylongemailaddressthatexceedsnormallimits@verylongdomainnamethatistoolong.com" in the email field
    And I trigger email validation
    Then I should see appropriate validation feedback based on email length constraints

  Scenario: Email validation after correcting invalid email
    When I enter email "invalid" in the email field
    And I trigger email validation
    Then I should see a message "Please enter a valid email address"
    When I correct the email to "valid@example.com"
    And I trigger email validation
    Then I should see a message "Email is available"
    And the email field should show a green checkmark