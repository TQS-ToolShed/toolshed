@owner-earnings
Feature: Owner Earnings Monitoring
  As an Owner
  I want to view my monthly earnings breakdown associated with completed bookings
  So that I can track my revenue performance over time

  Background:
    Given a user exists for earnings monitoring with email "earner@test.com" and role "SUPPLIER"

  @owner-earnings
  Scenario: Owner views monthly earnings breakdown
    Given the owner has the following completed bookings:
      | tool_name | month     | year | amount |
      | Drill     | MARCH     | 2024 | 100.0  |
      | Saw       | MARCH     | 2024 | 50.0   |
      | Drill     | FEBRUARY  | 2024 | 200.0  |
    When the owner requests their monthly earnings
    Then the earnings response should contain 2 entries
    And the earnings for "MARCH" "2024" should be 150.0
    And the earnings for "FEBRUARY" "2024" should be 200.0
