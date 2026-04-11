Feature: Post API

  Background:
    * url baseUrl
    * configure headers = { Accept: 'application/json', X-Workflow: karate.properties['karate.workflow'] }

  @smoke
  Scenario: Create a post returns 201
    * def payload = read('file:' + datasetPath + '/post-template.json')
    Given path 'posts'
    And request payload
    When method POST
    Then status 201
    And match response.id == '#number'
    And match response.title == payload.title

  @regression
  Scenario: Get post by ID returns expected fields
    Given path 'posts', '1'
    When method GET
    Then status 200
    And match response.id == 1
    And match response.userId == '#number'
    And match response.title == '#string'

  @contract @regression
  Scenario Outline: Validate multiple post ids
    Given path 'posts', <postId>
    When method GET
    Then status 200
    And match response.id == <postId>
    And match response.userId == '#number'
    And match response.title == '#string'
    Examples:
      | postId |
      | 1      |
      | 2      |

  @replay
  Scenario: Replay an incident snapshot from the dataset registry
    * def expected = read('file:' + datasetPath + '/post-1.json')
    Given path 'posts', expected.id
    When method GET
    Then status 200
    And match response contains expected
