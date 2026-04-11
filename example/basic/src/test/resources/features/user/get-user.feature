Feature: User API

  Background:
    * url baseUrl
    * configure connectTimeout = karate.properties['karate.config.connectTimeout'] || 3000
    * configure readTimeout = karate.properties['karate.config.readTimeout'] || 5000
    * configure retry = { count: 2, interval: 250 }

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
    * call read('classpath:features/helpers/assert-user.feature') { actual: '#(response)', expected: '#(expected)' }

  @contract @regression
  Scenario Outline: Validate a few user ids with one scenario
    Given path 'users', <id>
    When method GET
    Then status 200
    And match response.id == <id>
    And match response.username == '#string'
    Examples:
      | id |
      | 1  |
      | 2  |
