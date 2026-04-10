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
../gradlew :basic:regressionRun -Pworkflow=smoke -Penv=staging
../gradlew :basic:regressionRun -Pworkflow=contract -Pdataset=extended -Pcommit=abc123
../gradlew :wiremock:regressionRun -Pworkflow=regression -Pdataset=edge-cases
../gradlew regressionRunAll -Pworkflow=smoke
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

## DSL in use

```kotlin
// basic/build.gradle.kts
regression {
    workflowsDirs.add("src/test/resources/workflows")
    environmentsDirs.add("src/test/resources/environments")
    datasetsRootDir.set("src/test/resources/datasets")

    datasets {
        register("default")              { path.set("default") }
        register("extended")             { path.set("advanced") }
        register("incident-2026-04-09")  { path.set("incidents/2026-04-09") }
    }
}
```
