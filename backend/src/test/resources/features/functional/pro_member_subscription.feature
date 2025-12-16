@subscription
Feature: Pro Member Subscription
  As a renter
  I want to subscribe to Pro Member
  So that I can get discounts on all tool rentals

  Background:
    Given a registered renter exists

  Scenario: View subscription status for free user
    When I check my subscription status
    Then I should see tier "FREE"
    And I should see discount percentage 0

  Scenario: View subscription status for pro user
    Given I have an active Pro subscription
    When I check my subscription status
    Then I should see tier "PRO"
    And I should see discount percentage 5

  Scenario: Activate Pro subscription
    When I activate my Pro subscription with subscription ID "sub_cucumber_test"
    Then my subscription tier should be "PRO"
    And my subscription should be active

  Scenario: Pro member gets 5% discount on bookings
    Given I have an active Pro subscription
    And a tool "Power Drill" is available at 100 euros per day
    When I create a booking for 2 days
    Then the total price should be 190 euros

  Scenario: Free member pays full price on bookings
    Given a tool "Power Drill" is available at 100 euros per day
    When I create a booking for 2 days
    Then the total price should be 200 euros

  Scenario: Cancel Pro subscription
    Given I have an active Pro subscription
    When I cancel my subscription
    Then my subscription should be cancelled successfully

  Scenario: Cannot subscribe when already Pro
    Given I have an active Pro subscription
    When I try to subscribe to Pro again
    Then I should see an error about already being a Pro member
