Feature: Tool Maintenance Scheduling

  Scenario: Tool Owner successfully sets maintenance schedule
    Given the ToolShed application is running
    And I am logged in as an owner
    When I navigate to my tools page
    And I click the maintenance button for "Power Drill"
    And I select a date 5 days from now
    And I click "Set Maintenance"
    Then I should see the tool marked as "Under Maintenance"
    And the tool should be inactive

  Scenario: Renter cannot book a tool under maintenance
    Given the ToolShed application is running
    And there is a tool "Power Drill" under maintenance until 5 days from now
    And I am logged in as a renter
    When I view the details for "Power Drill"
    And I attempt to book it for tomorrow
    Then I should see an error message "Tool is under maintenance"
