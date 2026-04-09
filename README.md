# karate-gradle

A **Gradle-native, workflow-driven regression testing platform** built on top of [Karate](https://github.com/karatelabs/karate).

Reproducible test runs keyed on `(commit, workflow, dataset, env)` — identical inputs always produce identical results, locally and in CI.

---

## Features

- **Workflow DSL** — YAML-defined feature sets, tag filters, parallelism, and dataset bindings
- **Environment resolution** — `base.yaml` + `<env>.yaml` merge with runtime overrides
- **Dataset versioning** — named datasets with pluggable resolution (local filesystem today, S3 in the future)
- **Gradle-first** — one task, no CI-specific logic; Jenkins just passes parameters
- **Config-cache compatible** — designed for Gradle 8+ configuration cache (`warn` mode)

---

## Project Layout

```
karate-gradle/
├── core/          # Models, services, providers (no Gradle API dependency)
├── plugin/        # Gradle plugin: KarateRegressionPlugin + RegressionRunTask
└── buildSrc/      # Convention plugins (kotlin-jvm)
```

---

## Quickstart

### 1. Apply the plugin

```kotlin
// build.gradle.kts
plugins {
    id("org.openprojectx.karate.gradle") version "0.1.0"
}
```

The plugin automatically adds `io.karatelabs:karate-junit5` to `testImplementation`.

### 2. Configure the extension

```kotlin
regression {
    workflowsDir.set("src/test/resources/workflows")   // default
    environmentsDir.set("src/test/resources/environments") // default
    datasetsRootDir.set("datasets")                    // default

    datasets {
        register("default") {
            path.set("datasets/default")
        }
        register("incident-2026-04-09") {
            path.set("datasets/incidents/2026-04-09")
        }
    }
}
```

### 3. Create a workflow

```yaml
# src/test/resources/workflows/regression.yaml
name: regression

features:
  - classpath:features/user/**
  - classpath:features/payment/**

tags:
  include: ["@regression"]
  exclude: ["@ignore"]

env: prod
dataset: default
parallel: 10
```

### 4. Create environment files

```yaml
# src/test/resources/environments/base.yaml
timeout: 5000

# src/test/resources/environments/prod.yaml
baseUrl: https://api.prod.com
```

### 5. Run

```bash
./gradlew regressionRun \
  -Pworkflow=regression \
  -Penv=prod \
  -Pdataset=default \
  -Pcommit=abc123
```

Reports land in `build/reports/regression/`.

---

## Project Layout (Consumer)

```
your-project/
├── build.gradle.kts
│
├── src/test/resources/
│   ├── features/
│   │   ├── user/
│   │   └── payment/
│   │
│   ├── workflows/
│   │   ├── smoke.yaml
│   │   ├── regression.yaml
│   │   └── replay.yaml
│   │
│   ├── environments/
│   │   ├── base.yaml
│   │   ├── prod.yaml
│   │   └── staging.yaml
│   │
│   └── karate-config.js
│
└── datasets/
    ├── default/
    └── incidents/
```

---

## Workflow DSL Reference

| Field      | Type         | Default      | Description                                   |
|------------|--------------|--------------|-----------------------------------------------|
| `name`     | String       | required     | Workflow identifier                           |
| `features` | List<String> | `[]`         | Karate feature paths (`classpath:` supported) |
| `tags`     | Object       | none         | `include` and `exclude` tag lists             |
| `env`      | String       | `base`       | Target environment                            |
| `dataset`  | String       | `default`    | Dataset name                                  |
| `parallel` | Int          | `1`          | Number of parallel threads                    |
| `mode`     | String       | `standard`   | `standard` or `replay`                        |

### Replay Workflow

```yaml
name: replay
mode: replay
features:
  - classpath:features/replay/replay.feature
parallel: 1
```

---

## Task Parameters

All parameters are optional; workflow YAML values are used as defaults.

| Parameter     | Flag              | Description                     |
|---------------|-------------------|---------------------------------|
| workflow name | `-Pworkflow=`     | Which workflow YAML to load     |
| environment   | `-Penv=`          | Override workflow's `env` field |
| dataset       | `-Pdataset=`      | Override workflow's `dataset`   |
| commit ref    | `-Pcommit=`       | Set `karate.commit` sys prop    |

---

## Accessing Config in Features

System properties are forwarded to Karate automatically:

```javascript
// karate-config.js
function fn() {
    var config = {
        baseUrl: karate.properties['karate.config.baseUrl'],
        datasetPath: karate.properties['dataset.path']
    };
    return config;
}
```

```gherkin
# Reading dataset input
* def datasetPath = karate.properties['dataset.path']
* def input = read('file:' + datasetPath + '/case.json')
```

---

## Incident Replay Model

```
datasets/incidents/
  case-123/
    request.json
    response.json
    metadata.yaml
```

```gherkin
Scenario: replay incident
  Given url baseUrl
  And request read(datasetPath + '/request.json')
  When method POST
  Then match response == read(datasetPath + '/response.json')
```

---

## CI Integration (Jenkins)

No logic in Jenkins — just parameter passing:

```groovy
sh """
./gradlew regressionRun \
  -Pworkflow=${params.WORKFLOW} \
  -Penv=${params.ENV} \
  -Pdataset=${params.DATASET}
"""
```

---

## Extension DSL Reference

```kotlin
regression {
    workflowsDir.set("src/test/resources/workflows")
    featuresDir.set("src/test/resources/features")
    testDataDir.set("src/test/resources/test-data")
    environmentsDir.set("src/test/resources/environments")

    datasetProvider.set("local")   // "local" | "s3" (future)
    datasetsRootDir.set("datasets")

    datasets {
        register("default") {
            path.set("datasets/default")
        }
    }
}
```

---

## Building from Source

```bash
./gradlew :core:build :plugin:build
```

### Running Tests

```bash
# Core unit tests (WorkflowLoader, EnvResolver)
./gradlew :core:test

# Plugin integration tests (Gradle TestKit)
./gradlew :plugin:test
```

---

## Architecture

```
Gradle Task (regressionRun)
   ↓
WorkflowLoader          ← parses <name>.yaml
   ↓
DatasetResolver         ← LocalDatasetProvider (or future S3)
   ↓
EnvResolver             ← base.yaml + <env>.yaml merge
   ↓
KarateRunnerAdapter     ← builds CLI args
   ↓
ExecOperations.javaexec ← forks JVM, runs com.intuit.karate.Main
   ↓
build/reports/regression/
```

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
