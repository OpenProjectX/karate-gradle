Feature: Post API

  Background:
    * url baseUrl

  @smoke
  Scenario: Create a post returns 201
    Given path 'posts'
    And request { title: 'karate regression', body: 'deterministic testing', userId: 1 }
    When method POST
    Then status 201
    And match response.id == '#number'
    And match response.title == 'karate regression'

  @regression
  Scenario: Get post by ID returns expected fields
    Given path 'posts', '1'
    When method GET
    Then status 200
    And match response.id == 1
    And match response.userId == '#number'
    And match response.title == '#string'
