@review
Feature: Review Owner
  As a renter
  I want to review the owner after a booking is complete
  So that I can share my experience with others

  Scenario: Renter successfully reviews an owner
    Given a completed booking exists for a tool
    When the renter submits a review for the owner with rating 5 and comment "Excellent owner, very helpful!"
    Then the review should be created successfully
    And the review details should contain rating 5 and comment "Excellent owner, very helpful!"
