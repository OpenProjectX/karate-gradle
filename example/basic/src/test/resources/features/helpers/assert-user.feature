@ignore
Feature: User Assertions

  Scenario:
    * def actual = __arg.actual
    * def expected = __arg.expected
    * match actual contains deep expected
    * match actual.email == '#regex .+@.+'
    * match actual.company == '#object'
