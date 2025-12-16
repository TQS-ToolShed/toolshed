@deposit-payment
Feature: Deposit Payment
  As a renter
  I want to pay a deposit when required after damaging a tool
  So that the owner can be compensated for damages

  Background:
    Given a completed booking exists with required deposit

  Scenario: Renter successfully pays deposit
    When the renter pays the deposit
    Then the deposit should be marked as paid
    And the deposit payment timestamp should be recorded

  Scenario: Cannot pay deposit when not required
    Given a completed booking exists with no deposit required
    When the renter tries to pay the deposit
    Then the payment should fail with deposit not required error

  Scenario: Cannot pay deposit twice
    When the renter pays the deposit
    And the renter tries to pay the deposit again
    Then the payment should fail with deposit already paid error

  Scenario: Full flow - damage reported to deposit paid
    Given a completed booking exists with paid rental
    When the renter submits a condition report with status "BROKEN" and description "Tool is broken"
    Then the deposit status should be "REQUIRED"
    When the renter pays the deposit
    Then the deposit should be marked as paid
    And the final booking state shows condition "BROKEN" and deposit "PAID"
