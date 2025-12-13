@e2e
Feature: User Registration
  As a new user
  I want to register on the ToolShed platform
  So that I can create an account and access the platform services

  Background:
    Given the ToolShed application is running
    And I am on the registration page

  Scenario: Successful registration with valid data
    When I fill in the registration form with:
      | firstName | John                |
      | lastName  | Doe                 |
      | email     | newuser@example.com |
      | password  | SecurePass123!      |
      | role      | RENTER              |
    And I click the register button
    Then I should see a success message "Registration successful"
    And I should be redirected to the login page

  Scenario: Registration with existing email
    Given a user with email "existing@example.com" already exists
    When I fill in the registration form with:
      | firstName | Jane                 |
      | lastName  | Smith                |
      | email     | existing@example.com |
      | password  | SecurePass123!       |
      | role      | SUPPLIER             |
    And I click the register button
    Then I should see an error message "Email already exists"
    And I should remain on the registration page

  Scenario: Registration with empty first name
    When I fill in the registration form with:
      | firstName |                     |
      | lastName  | Doe                 |
      | email     | newuser@example.com |
      | password  | SecurePass123!      |
      | role      | RENTER              |
    And I click the register button
    Then I should see a validation error "First name is required"
    And I should remain on the registration page

  Scenario: Registration with empty last name
    When I fill in the registration form with:
      | firstName | John                |
      | lastName  |                     |
      | email     | newuser@example.com |
      | password  | SecurePass123!      |
      | role      | RENTER              |
    And I click the register button
    Then I should see a validation error "Last name is required"
    And I should remain on the registration page

  Scenario: Registration with invalid email format
    When I fill in the registration form with:
      | firstName | John           |
      | lastName  | Doe            |
      | email     | invalid-email  |
      | password  | SecurePass123! |
      | role      | RENTER         |
    And I click the register button
    Then I should see a validation error "Please enter a valid email address"
    And I should remain on the registration page

  Scenario: Registration with empty email
    When I fill in the registration form with:
      | firstName | John           |
      | lastName  | Doe            |
      | email     |                |
      | password  | SecurePass123! |
      | role      | RENTER         |
    And I click the register button
    Then I should see a validation error "Email is required"
    And I should remain on the registration page

  Scenario: Registration with short password
    When I fill in the registration form with:
      | firstName | John                |
      | lastName  | Doe                 |
      | email     | newuser@example.com |
      | password  | 123                 |
      | role      | RENTER              |
    And I click the register button
    Then I should see a validation error "Password must be at least 8 characters long"
    And I should remain on the registration page

  Scenario: Registration with empty password
    When I fill in the registration form with:
      | firstName | John                |
      | lastName  | Doe                 |
      | email     | newuser@example.com |
      | password  |                     |
      | role      | RENTER              |
    And I click the register button
    Then I should see a validation error "Password is required"
    And I should remain on the registration page

  Scenario: Registration without selecting role
    When I fill in the registration form with:
      | firstName | John                |
      | lastName  | Doe                 |
      | email     | newuser@example.com |
      | password  | SecurePass123!      |
      | role      |                     |
    And I click the register button
    Then I should see a validation error "Please select a role"
    And I should remain on the registration page

  Scenario: Registration as SUPPLIER
    When I fill in the registration form with:
      | firstName | Jane                |
      | lastName  | Smith               |
      | email     | supplier@example.com|
      | password  | SecurePass123!      |
      | role      | SUPPLIER            |
    And I click the register button
    Then I should see a success message "Registration successful"
    And I should be redirected to the login page