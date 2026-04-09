Feature: WireMock bootstrap

  Scenario:
    Given url baseUrl

    And path '__admin', 'reset'
    When method POST
    Then status 200

    * def account = read('file:' + datasetPath + '/account-1001.json')
    * def paymentRequest = read('file:' + datasetPath + '/payment-request.json')

    * def accountMapping =
    """
    {
      request: {
        method: 'GET',
        urlPathPattern: '/accounts/([^/]+)'
      },
      response: {
        status: 200,
        headers: {
          'Content-Type': 'application/json'
        },
        jsonBody: #(account)
      }
    }
    """
    Given path '__admin', 'mappings'
    And request accountMapping
    When method POST
    Then status 201

    * def submitPaymentMapping =
    """
    {
      request: {
        method: 'POST',
        url: '/payments'
      },
      response: {
        status: 202,
        headers: {
          'Content-Type': 'application/json'
        },
        jsonBody: {
          paymentId: '#(paymentRequest.paymentId)',
          state: 'PENDING',
          receivedAmount: '#(paymentRequest.amount)'
        }
      }
    }
    """
    Given path '__admin', 'mappings'
    And request submitPaymentMapping
    When method POST
    Then status 201

    * def pendingStatusMapping =
    """
    {
      scenarioName: 'payment-status',
      requiredScenarioState: 'Started',
      newScenarioState: 'APPROVED_READY',
      request: {
        method: 'GET',
        url: '#("/payments/" + paymentRequest.paymentId + "/status")'
      },
      response: {
        status: 200,
        headers: {
          'Content-Type': 'application/json'
        },
        jsonBody: {
          paymentId: '#(paymentRequest.paymentId)',
          state: 'PENDING'
        }
      }
    }
    """
    Given path '__admin', 'mappings'
    And request pendingStatusMapping
    When method POST
    Then status 201

    * def approvedStatusMapping =
    """
    {
      scenarioName: 'payment-status',
      requiredScenarioState: 'APPROVED_READY',
      request: {
        method: 'GET',
        url: '#("/payments/" + paymentRequest.paymentId + "/status")'
      },
      response: {
        status: 200,
        headers: {
          'Content-Type': 'application/json'
        },
        jsonBody: {
          paymentId: '#(paymentRequest.paymentId)',
          state: 'APPROVED'
        }
      }
    }
    """
    Given path '__admin', 'mappings'
    And request approvedStatusMapping
    When method POST
    Then status 201
