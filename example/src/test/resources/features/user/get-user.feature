Feature: User API

  Background:
    * url baseUrl

  @smoke @regression
  Scenario: Get user by ID returns expected fields
    Given path 'users', '1'
    When method GET
    Then status 200
    And match response.id == 1
    And match response.name == '#string'
    And match response.email == '#string'

  @smoke @regression
  Scenario: Get all users returns a list
    Given path 'users'
    When method GET
    Then status 200
    And match response == '#[] #notnull'
    And assert response.length > 0

  @regression
  Scenario: Get user matches contract data from dataset
    * def expected = read('file:' + datasetPath + '/user-1.json')
    Given path 'users', '1'
    When method GET
    Then status 200
    And match response.id == expected.id
    And match response.name == expected.name
