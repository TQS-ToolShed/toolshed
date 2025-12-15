@condition-report
Feature: Condition Report Submission
  As a renter
  I want to submit a condition report after returning a tool
  So that any damage can be recorded and a deposit required if necessary

  Background:
    Given a completed booking exists with paid rental

  Scenario: Renter reports tool in OK condition - no deposit required
    When the renter submits a condition report with status "OK" and description "Tool returned in perfect condition"
    Then the condition report should be submitted successfully
    And the deposit status should be "NOT_REQUIRED"
    And the deposit amount should be 0.0

  Scenario: Renter reports tool is BROKEN - deposit required
    When the renter submits a condition report with status "BROKEN" and description "Tool is completely broken"
    Then the condition report should be submitted successfully
    And the deposit status should be "REQUIRED"
    And the deposit amount should be 8.0

  Scenario: Renter reports MINOR_DAMAGE - deposit required
    When the renter submits a condition report with status "MINOR_DAMAGE" and description "Small scratch on handle"
    Then the condition report should be submitted successfully
    And the deposit status should be "REQUIRED"
    And the deposit amount should be 8.0

  Scenario: Renter reports MISSING_PARTS - deposit required
    When the renter submits a condition report with status "MISSING_PARTS" and description "Drill bits are missing"
    Then the condition report should be submitted successfully
    And the deposit status should be "REQUIRED"
    And the deposit amount should be 8.0

  Scenario: Renter reports tool is USED - no deposit required
    When the renter submits a condition report with status "USED" and description "Normal wear and tear"
    Then the condition report should be submitted successfully
    And the deposit status should be "NOT_REQUIRED"
    And the deposit amount should be 0.0

  Scenario: Cannot submit condition report for non-completed booking
    Given a booking exists that is not completed
    When the renter tries to submit a condition report
    Then the submission should fail with status 400

  Scenario: Cannot submit duplicate condition report
    When the renter submits a condition report with status "OK" and description "First report"
    And the renter tries to submit another condition report
    Then the submission should fail with status 400
