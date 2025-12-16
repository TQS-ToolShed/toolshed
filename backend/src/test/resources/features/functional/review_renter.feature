@review
Feature: Review Renter
  As an owner
  I want to review the renter after a booking is complete
  So that I can rate their behavior

  Scenario: Owner successfully reviews a renter
    Given a completed booking exists for a tool
    When the owner submits a review for the renter with rating 4 and comment "Good renter, returned on time."
    Then the review should be created successfully
    And the review details should contain rating 4 and comment "Good renter, returned on time."
