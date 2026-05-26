Feature: Login once and return a bearer token

  Scenario:
    Given url baseUrl
    And path 'login'
    And request { username: '#(username)', password: '#(password)' }
    When method POST
    Then status 200
    And match response.token == '#string'
    * def token = response.token
