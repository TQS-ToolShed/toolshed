@owner-payout
Feature: Owner Payout Management
  As an Owner
  I want to view my accumulated balance and request payouts
  So that I can receive my earnings from tool rentals

  Background:
    Given a user exists with email "owner@test.com" and role "SUPPLIER"
    And the owner has wallet balance of 150.0 euros

  @owner-payout
  Scenario: Owner views their wallet balance
    When the owner requests their wallet information
    Then the wallet response should contain balance 150.0
    And the wallet response should contain recent payouts list

  @owner-payout
  Scenario: Owner successfully requests a payout
    When the owner requests a payout of 100.0 euros
    Then the payout request should be successful
    And the payout status should be "COMPLETED"
    And the owner's wallet balance should be 50.0 euros

  @owner-payout
  Scenario: Owner requests payout exceeding balance
    When the owner requests a payout of 200.0 euros
    Then the payout request should fail with "Insufficient balance"

  @owner-payout
  Scenario: Owner requests payout with invalid amount
    When the owner requests a payout of -50.0 euros
    Then the payout request should fail with "positive"

  @owner-payout
  Scenario: Balance increases when booking is paid
    Given a booking exists for a tool owned by the owner with total price 75.0
    When the booking is marked as paid
    Then the owner's wallet balance should be 225.0 euros

  @owner-payout
  Scenario: Owner views payout history
    Given the owner has made previous payouts
    When the owner requests their payout history
    Then the payout history should contain the previous payouts
