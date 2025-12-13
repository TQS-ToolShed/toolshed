@review
Feature: Review Tool
  As a renter
  I want to review the tool after a booking is complete
  So that I can share my experience with others

  Scenario: Renter successfully reviews a tool
    Given a completed booking exists for a tool
    When the renter submits a review for the tool with rating 5 and comment "Excellent tool, worked perfectly!"
    Then the review should be created successfully
    And the review details should contain rating 5 and comment "Excellent tool, worked perfectly!"
