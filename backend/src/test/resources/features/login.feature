Feature: User Login
  As a registered user
  I want to log in to the ToolShed platform
  So that I can access my account and use the platform features

  Background:
    Given the ToolShed application is running
    And I am on the login page

  Scenario: Successful login with valid credentials
    When I enter email "user@example.com" and password "validPassword"
    And I click the login button
    Then I should be redirected to the dashboard
    And I should see a welcome message

  Scenario: Login with invalid email
    When I enter email "invalid@example.com" and password "validPassword"
    And I click the login button
    Then I should see an error message "Invalid credentials"
    And I should remain on the login page

  Scenario: Login with invalid password
    When I enter email "user@example.com" and password "wrongPassword"
    And I click the login button
    Then I should see an error message "Invalid credentials"
    And I should remain on the login page

  Scenario: Login with empty email field
    When I enter email "" and password "validPassword"
    And I click the login button
    Then I should see a validation error "Email is required"
    And I should remain on the login page

  Scenario: Login with empty password field
    When I enter email "user@example.com" and password ""
    And I click the login button
    Then I should see a validation error "Password is required"
    And I should remain on the login page

  Scenario: Login with invalid email format
    When I enter email "invalid-email" and password "validPassword"
    And I click the login button
    Then I should see a validation error "Please enter a valid email address"
    And I should remain on the login page

  Scenario: Login with suspended account
    Given a user with email "suspended@example.com" has status "SUSPENDED"
    When I enter email "suspended@example.com" and password "validPassword"
    And I click the login button
    Then I should see an error message "Account is suspended"
    And I should remain on the login page

  Scenario: Login with pending verification account
    Given a user with email "pending@example.com" has status "PENDING_VERIFICATION"
    When I enter email "pending@example.com" and password "validPassword"
    And I click the login button
    Then I should see an error message "Account is not verified"
    And I should remain on the login page