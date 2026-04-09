Feature: Payment lifecycle against WireMock

  Background:
    * callonce read('classpath:features/helpers/bootstrap.feature')
    * url baseUrl
    * configure retry = { count: 3, interval: '#(pollIntervalMillis)' }
    * def account = read('file:' + datasetPath + '/account-1001.json')
    * def paymentRequest = read('file:' + datasetPath + '/payment-request.json')

  @smoke @regression
  Scenario: Read account details from the stubbed service
    Given path 'accounts', account.id
    When method GET
    Then status 200
    And match response == read('classpath:schemas/account-schema.json')
    And match response contains deep account

  @regression @contract
  Scenario: Submit a payment and poll until it is approved
    * def requestBody = karate.merge(paymentRequest, { submittedBy: karate.properties['karate.config.tenant'] })
    Given path 'payments'
    And request requestBody
    When method POST
    Then status 202
    And match response ==
    """
    {
      paymentId: '#(paymentRequest.paymentId)',
      state: 'PENDING',
      receivedAmount: '#number'
    }
    """

    Given path 'payments', paymentRequest.paymentId, 'status'
    And retry until response.state == 'APPROVED'
    When method GET
    Then status 200
    And match response ==
    """
    {
      paymentId: '#(paymentRequest.paymentId)',
      state: 'APPROVED'
    }
    """
