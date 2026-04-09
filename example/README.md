# karate-example

Standalone composite consumer build for `karate-gradle`.

## Modules

- `basic` demonstrates workflow selection, dataset registration, environment layering, tag filtering, and dataset overrides against a public HTTP API.
- `wiremock` demonstrates local-service testing with WireMock plus more advanced Karate patterns such as reusable setup, polling, data shaping, and schema assertions.

## Run

From `example/`:

```bash
../gradlew :basic:regressionRun -Pworkflow=smoke -Penv=staging
../gradlew :basic:regressionRun -Pworkflow=contract -Pdataset=extended -Pcommit=abc123
../gradlew :wiremock:regressionRun -Pworkflow=regression -Pdataset=edge-cases
../gradlew regressionRunAll -Pworkflow=smoke
```

Each module keeps its own `src/test/resources` layout so the examples stay close to a real consumer project.
