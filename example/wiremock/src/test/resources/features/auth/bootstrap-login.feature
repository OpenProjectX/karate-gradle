Feature: Register the WireMock login stub

  Scenario:
    Given url baseUrl

    * def authToken = 'wiremock-demo-token'
    * def loginMapping =
    """
    {
      request: {
        method: 'POST',
        url: '/login'
      },
      response: {
        status: 200,
        headers: {
          'Content-Type': 'application/json'
        },
        jsonBody: {
          token: '#(authToken)',
          tokenType: 'Bearer'
        }
      }
    }
    """
    Given path '__admin', 'mappings'
    And request loginMapping
    When method POST
    Then status 201
