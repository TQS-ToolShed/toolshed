Feature: Tool Images Support

  Background:
    Given a supplier "supplier@example.com" exists with password "password"

  Scenario: Supplier can add a tool with an image URL
    Given I am authenticated as "supplier@example.com" with password "password"
    When I create a tool "Drill with Image" with description "Has a photo" and price 15.0 and image "http://example.com/drill.jpg"
    Then the tool "Drill with Image" should exist in my tools list
    And the tool "Drill with Image" should have the image URL "http://example.com/drill.jpg"

  Scenario: Users can view tool details including the image
    Given a tool "Hammer" exists with image "http://example.com/hammer.jpg" owned by "supplier@example.com"
    When I request the details for the tool "Hammer"
    Then the response should contain the image URL "http://example.com/hammer.jpg"
