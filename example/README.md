# karate-example

Standalone composite consumer build for `karate-gradle`.

## Modules

- `basic` demonstrates workflow selection, dataset registration, environment layering, tag filtering,
  and dataset overrides against a public HTTP API.
- `wiremock` demonstrates local-service testing with WireMock plus more advanced Karate patterns
  such as reusable setup, polling, data shaping, and schema assertions.

## Run

From `example/`:

```bash
# Workflow-driven runs (Karate HTML report → build/reports/regression/)
../gradlew :basic:regressionRun -Pworkflow=smoke -Penv=staging
../gradlew :basic:regressionRun -Pworkflow=contract -Pdataset=extended -Pcommit=abc123
../gradlew :wiremock:regressionRun -Pworkflow=regression -Pdataset=edge-cases
../gradlew regressionRunAll -Pworkflow=smoke

# Allure HTML report (basic module only — requires io.qameta.allure plugin)
../gradlew :basic:test allureReport          # generates build/reports/allure-report/
../gradlew :basic:allureServe                # generates + opens in browser
```

Each module keeps its own `src/test/resources` layout so the examples stay close to a real consumer project.

## Config format

Both HOCON (`.conf`) and YAML (`.yaml`) are supported. The modules intentionally use both
formats to show that they work side by side within the same project.

```
basic/src/test/resources/
├── workflows/
│   ├── smoke.conf        ← HOCON
│   ├── regression.conf   ← HOCON
│   ├── contract.conf     ← HOCON
│   └── replay.conf       ← HOCON
└── environments/
    ├── base.conf
    ├── staging.conf
    └── production.conf

wiremock/src/test/resources/
├── workflows/
│   ├── smoke.yaml        ← YAML (same capability, different format)
│   └── regression.conf   ← HOCON
└── environments/
    ├── base.conf
    └── local.conf
```

Discovery order within a directory: `.conf` → `.json` → `.yaml` → `.yml` → `.properties`.

## Reporting integrations

Both modules demonstrate opt-in reporting integrations. Reporting agents hook into JUnit5
lifecycle events — they work via `./gradlew test`, not via `regressionRun`.

```
regressionRun  →  com.intuit.karate.Main  →  build/reports/regression/  (Karate HTML)
test           →  JUnit5  →  allure-junit5  →  build/allure-results/    (Allure JSON)
                         →  allureReport   →  build/reports/allure-report/ (Allure HTML)
                         →  agent-junit5   →  streams to ReportPortal server
```

### Allure (`basic`)

`basic` demonstrates Allure HTML report generation:

| Task | Executor | Reports |
|------|----------|---------|
| `regressionRun` | `com.intuit.karate.Main` | Karate HTML → `build/reports/regression/` |
| `test` + `allureReport` | JUnit5 via `AllureRunner` | Allure HTML → `build/reports/allure-report/` |

The plugin auto-generates a `KarateRunner` class into `build/generated-test-sources/karate-gradle/`
when any reporter is enabled. No runner class is needed in the consumer project.

### ReportPortal (`wiremock`)

`wiremock` demonstrates ReportPortal real-time reporting. ReportPortal streams results to
a running server — it has no local HTML output mode.

```bash
# Start a local ReportPortal instance (optional)
docker-compose up -d   # see https://github.com/reportportal/reportportal#deployment

# Export credentials
export RP_ENDPOINT=http://localhost:8080
export RP_API_KEY=your-api-key

# Enable in build.gradle.kts:
#   reporting { reportPortal { enabled.set(true) } }

# Run tests (streams results to ReportPortal in real time)
../gradlew :wiremock:test
```

WireMock is started automatically by `karate-config.js` via `WireMockSupport.ensureStarted()`.

## DSL in use

```kotlin
// basic/build.gradle.kts
plugins {
    id("org.openprojectx.karate.gradle")
    id("io.qameta.allure") version "3.0.1"
}

regression {
    workflowsDirs.add("src/test/resources/workflows")
    environmentsDirs.add("src/test/resources/environments")
    datasetsRootDir.set("src/test/resources/datasets")

    reporting {
        allure { enabled.set(true) }
    }

    datasets {
        register("default")              { path.set("default") }
        register("extended")             { path.set("advanced") }
        register("incident-2026-04-09")  { path.set("incidents/2026-04-09") }
    }
}
```
