@refund
Feature: Booking Cancellation and Refunds
  As a Renter
  I want to receive a refund automatically when I cancel a booking
  So that the process is frictionless

  Background:
    Given a renter with email "renter@toolshed.com"
    And an owner with email "owner@toolshed.com" and wallet balance 0.0
    And a tool "Power Drill" owned by the owner priced at 10.0 per day
    And a booking for the tool from the renter with total price 100.0 and status "APPROVED"

  Scenario: Full refund when cancelling 7+ days before start date
    Given the booking starts in 10 days
    When the renter cancels the booking
    Then the booking status should be "CANCELLED"
    And the refund percentage should be 100
    And the owner wallet balance should be 0.0

  Scenario: Partial refund (50%) when cancelling 3-6 days before start date
    Given the booking starts in 5 days
    When the renter cancels the booking
    Then the booking status should be "CANCELLED"
    And the refund percentage should be 50
    And the owner wallet balance should be 50.0

  Scenario: Partial refund (25%) when cancelling 1-2 days before start date
    Given the booking starts in 2 days
    When the renter cancels the booking
    Then the booking status should be "CANCELLED"
    And the refund percentage should be 25
    And the owner wallet balance should be 75.0

  Scenario: No refund when cancelling on same day
    Given the booking starts today
    When the renter cancels the booking
    Then the booking status should be "CANCELLED"
    And the refund percentage should be 0
    And the owner wallet balance should be 100.0

  Scenario: Cannot cancel booking by wrong renter
    Given another renter with email "other@toolshed.com"
    And the booking starts in 10 days
    When the other renter tries to cancel the booking
    Then a forbidden error should be returned

  Scenario: Cannot cancel already cancelled booking
    Given the booking starts in 10 days
    And the booking is already cancelled
    When the renter cancels the booking
    Then a bad request error should be returned
