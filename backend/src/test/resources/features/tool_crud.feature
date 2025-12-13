@tool
Feature: Tool Management
  As a user
  I want to manage tools
  So that I can share or rent them

  Scenario: Create a new tool
    Given I am on the create tool page
    When I fill in the tool details with title "BDD Drill", description "Powerful BDD Drill", price "20.0", location "BDD City"
    And I submit the form
    Then I should see "BDD Drill" in the tool list

  Scenario: Search for a tool
    Given there is a tool with title "Searchable Saw"
    When I search for "Searchable"
    Then I should see "Searchable Saw" in the results
