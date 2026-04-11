# karate-gradle

A **Gradle-native, workflow-driven regression testing platform** built on top of [Karate](https://github.com/karatelabs/karate).

Reproducible test runs keyed on `(commit, workflow, dataset, env)` — identical inputs always produce identical results, locally and in CI.

---

## Features

- **Workflow DSL** — HOCON/YAML/JSON-defined feature sets, tag filters, parallelism, and dataset bindings
- **Environment resolution** — `base` + `<env>` config merge for flat scalar config
- **Multi-source config** — search multiple directories per source type; first match wins
- **Dataset versioning** — named datasets resolved from the local filesystem today; S3 / Git can be added later
- **Reporting integrations** — built-in opt-in support for Allure and ReportPortal; no boilerplate
- **Gradle-first** — one task, no CI-specific logic; Jenkins just passes parameters
- **Config-cache compatible** — designed for Gradle 8+ configuration cache (`warn` mode)

---

## Migration from 0.1.x → 0.2.x (Breaking Changes)

### Extension DSL

The `workflowsDir` and `environmentsDir` single-directory properties are replaced by
`workflowsDirs` and `environmentsDirs` list properties that accept one or more directories.

```kotlin
// BEFORE (0.1.x)
regression {
    workflowsDir.set("src/test/resources/workflows")
    environmentsDir.set("src/test/resources/environments")
    featuresDir.set("src/test/resources/features")   // existed but unused
    testDataDir.set("src/test/resources/test-data")  // existed but unused
}

// AFTER (0.2.x)
regression {
    workflowsDirs.add("src/test/resources/workflows")
    environmentsDirs.add("src/test/resources/environments")
    // featuresDir and testDataDir removed
}
```

`add()` appends to the list; `set(listOf(...))` replaces it entirely. Multiple directories
are searched in order — the first directory containing a match wins.

### Config file format

YAML (`.yaml` / `.yml`) is still fully supported. The preferred format is now HOCON (`.conf`),
which is the default when both exist.

**Discovery order per directory:** `.conf` → `.json` → `.yaml` → `.yml` → `.properties`

If you keep YAML files, no change is needed. To migrate a workflow file:

```yaml
# regression.yaml  (still works)
name: regression
tags:
  include: ["@regression"]
  exclude: ["@ignore"]
```

```hocon
# regression.conf  (preferred)
name = regression
tags {
  "include" = ["@regression"]   # note: include is a HOCON reserved keyword — quote it
  exclude = ["@ignore"]
}
```

---

## Project Layout

```
karate-gradle/
├── core/          # Models, services, providers (no Gradle API dependency)
├── plugin/        # Gradle plugin: KarateRegressionPlugin + RegressionRunTask
├── example/       # Standalone consumer build with multiple sample submodules
└── buildSrc/      # Convention plugins (kotlin-jvm)
```

### Running the example

The `example/` build uses `includeBuild("..")` to apply the plugin directly from
source — no publishing needed. It contains:

- `basic` for workflow, dataset, and environment examples against a public API
- `wiremock` for a local stubbed-service example with more advanced Karate flows

```bash
cd example
../gradlew :basic:regressionRun -Pworkflow=smoke -Penv=staging
../gradlew :wiremock:regressionRun -Pworkflow=regression
```

---

## Quickstart

### 1. Apply the plugin

```kotlin
// build.gradle.kts
plugins {
    id("org.openprojectx.karate.gradle") version "0.1.69-SNAPSHOT"
}
```

The plugin automatically adds `io.karatelabs:karate-junit5` to `testImplementation`.

### 2. Configure the extension

```kotlin
regression {
    // Directories are searched in order; first match wins.
    // Default: ["src/test/resources/workflows"]
    workflowsDirs.add("src/test/resources/workflows")

    // Default: ["src/test/resources/environments"]
    environmentsDirs.add("src/test/resources/environments")

    // Local filesystem only for now.
    datasetProvider.set("local")

    datasetsRootDir.set("datasets")   // default

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

HOCON (`.conf`) is preferred; YAML (`.yaml`) is also supported.

```hocon
# src/test/resources/workflows/regression.conf
name = regression

features = [
  "classpath:features/user/**"
  "classpath:features/payment/**"
]

tags {
  "include" = ["@regression"]   # include is a HOCON reserved word — quote it
  exclude   = ["@ignore"]
}

env      = prod
dataset  = default
parallel = 10
```

<details>
<summary>Same workflow in YAML</summary>

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

</details>

### 4. Create environment files

```hocon
# src/test/resources/environments/base.conf
timeout = 5000

# src/test/resources/environments/prod.conf
baseUrl = "https://api.prod.com"
```

Environment files are currently intended to stay flat and scalar.
Good fits are values like URLs, tenant names, timeouts, and simple flags.

```hocon
# recommended
timeout = 5000
baseUrl = "https://api.prod.com"
tenant = "public-prod"
```

Nested objects and lists are not an official part of the current contract.
They may parse, but the plugin currently forwards env config to Karate as flattened
`karate.config.*` properties, so complex structures are not guaranteed to behave as
real nested objects or lists inside Karate.

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
│   │   ├── smoke.conf
│   │   ├── regression.conf
│   │   └── replay.conf
│   │
│   ├── environments/
│   │   ├── base.conf
│   │   ├── prod.conf
│   │   └── staging.conf
│   │
│   └── karate-config.js
│
└── datasets/
    ├── default/
    └── incidents/
```

---

## Workflow DSL Reference

Supported formats: `.conf` (HOCON), `.json`, `.yaml`, `.yml`, `.properties`.

| Field      | Type         | Default      | Description                                        |
|------------|--------------|--------------|----------------------------------------------------|
| `name`     | String       | required     | Workflow identifier                                |
| `features` | List<String> | `[]`         | Karate feature paths (`classpath:` supported)      |
| `tags`     | Object       | none         | `include` and `exclude` tag lists (see note below) |
| `env`      | String       | `base`       | Target environment                                 |
| `dataset`  | String       | `default`    | Dataset name                                       |
| `parallel` | Int          | `1`          | Number of parallel threads                         |
| `mode`     | String       | `standard`   | `standard` or `replay`                             |

> **HOCON note:** `include` is a reserved keyword in HOCON. Inside a `tags {}` block,
> write `"include" = [...]` (with quotes). YAML files are not affected.

## Environment Config Contract

Environment files are merged as `base` + `<env>`, with the selected env overriding `base`.

Current recommendation:

- Keep environment files flat.
- Use scalar values only: strings, numbers, booleans.
- Treat nested objects and arrays as unsupported for now.

This keeps the contract aligned with how values are currently passed into Karate via
flattened `karate.config.*` system properties.

### Replay Workflow

```hocon
# replay.conf
name    = replay
mode    = replay
features = ["classpath:features/replay/replay.feature"]
parallel = 1
```

---

## Multi-Source Config

Each of `workflowsDirs` and `environmentsDirs` accepts multiple directories.
Sources are resolved in list order — the **first directory** containing a matching file wins.

```kotlin
regression {
    // Shared org-level workflows are checked first, project-level second
    workflowsDirs.set(listOf(
        "config/shared/workflows",
        "src/test/resources/workflows",
    ))

    environmentsDirs.set(listOf(
        "config/shared/environments",
        "src/test/resources/environments",
    ))
}
```

This is useful for inheriting a shared base config from a corporate config repo while
allowing project-level overrides.

---

## Task Parameters

All parameters are optional; workflow file values are used as defaults.

| Parameter     | Flag          | Description                     |
|---------------|---------------|---------------------------------|
| workflow name | `-Pworkflow=` | Which workflow file to load     |
| environment   | `-Penv=`      | Override workflow's `env` field |
| dataset       | `-Pdataset=`  | Override workflow's `dataset`   |
| commit ref    | `-Pcommit=`   | Set `karate.commit` sys prop    |

---

## Accessing Config in Features

System properties are forwarded to Karate automatically:

```javascript
// karate-config.js
function fn() {
    var config = {
        workflow:    karate.properties['karate.workflow'],
        baseUrl:     karate.properties['karate.config.baseUrl'],
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

## Reporting

### How reporting works

`regressionRun` forks a JVM and launches the generated `KarateJUnit5Launcher` on the
consumer test classpath. It runs the configured workflow through JUnit Platform and
generates Karate's HTML report in `build/reports/regression/`.

Allure and ReportPortal both work by hooking into JUnit5 lifecycle events. Their results
are therefore available anywhere the generated JUnit entry point runs, including
`regressionRun` and the standard **`test`** Gradle task.

``` 
regressionRun  →  KarateJUnit5Launcher  →  JUnit Platform  →  build/reports/regression/  (Karate HTML)
                                                     →  allure-junit5  →  build/allure-results/ (Allure JSON)
                                                     →  agent-junit5   →  streams to ReportPortal server
test           →  Gradle Test          →  JUnit Platform  →  allure-junit5  →  build/allure-results/ (Allure JSON)
                                                     →  agent-junit5   →  streams to ReportPortal server
allureReport   →  build/allure-results/              →  build/reports/allure-report/       (Allure HTML)
```

The two flows are complementary: `regressionRun` adds workflow-aware inputs such as
dataset, environment, and commit parameters, while `test` remains the standard Gradle
entry point for plain JUnit execution. Both can feed listener-based integrations.

---

### Allure (local HTML report)

```kotlin
// build.gradle.kts
plugins {
    id("org.openprojectx.karate.gradle")
    id("io.qameta.allure") version "3.0.1"   // adds allureReport + allureServe tasks
}

allure {
    adapter { autoconfigure.set(false) }   // our plugin manages the allure-junit5 dep
}

regression {
    reporting {
        allure {
            enabled.set(true)
            // version.set("2.27.0")           // default
            // resultsDir.set("allure-results") // relative to build dir, default
        }
    }
}
```

When `allure.enabled = true` the plugin:
1. Adds `io.qameta.allure:allure-junit5` to `testImplementation`
2. Sets `-Dallure.results.directory` on every `Test` task so results land in the right place
3. Generates `KarateRunner` and `KarateJUnit5Launcher` into
   `build/generated-test-sources/karate-gradle/` and adds them to the `test` source set
   — no runner class needed in the consumer project

The features path used by the generated runner defaults to `classpath:features` (standard
Karate convention). Override it if your layout differs:

```kotlin
regression {
    reporting {
        featuresPath.set("classpath:myfeatures")
        allure { enabled.set(true) }
    }
}
```

```bash
# Run features and generate Allure HTML report
./gradlew test allureReport

# Open the report in the browser (downloads Allure CLI automatically)
./gradlew allureServe
```

The HTML report is at `build/reports/allure-report/index.html`.

---

### ReportPortal (remote, real-time)

ReportPortal is a remote test management platform — it streams results to a running server
in real time; it does not generate a local HTML file. You can run it locally via Docker:

```bash
docker-compose -f docker-compose.yml up -d   # official RP docker-compose
```

```kotlin
regression {
    reporting {
        reportPortal {
            enabled.set(true)
            endpoint.set("https://reportportal.example.com")
            apiKey.set(providers.environmentVariable("RP_API_KEY"))  // keep out of VCS
            project.set("karate-regression")
            launch.set("smoke")
            description.set("Nightly smoke suite")
            attributes.set(listOf("team:backend", "env:staging"))
        }
    }
}
```

When `reportPortal.enabled = true` the plugin:
1. Adds `com.epam.reportportal:agent-java-junit5` to `testImplementation`
2. Sets all `rp.*` system properties on every `Test` task so the agent streams results
3. Uses the same generated `KarateRunner` / `KarateJUnit5Launcher` entry point as
   `regressionRun` and `test` — no runner class needed

```bash
./gradlew test   # runs tests and streams results to ReportPortal
```

| Property      | System property  | Description                                      |
|---------------|------------------|--------------------------------------------------|
| `endpoint`    | `rp.endpoint`    | ReportPortal server URL                          |
| `apiKey`      | `rp.api.key`     | API key — supply via env var, not hardcoded      |
| `project`     | `rp.project`     | Project name in ReportPortal                     |
| `launch`      | `rp.launch`      | Launch name displayed in the UI                  |
| `description` | `rp.description` | Optional launch description                      |
| `attributes`  | `rp.attributes`  | `key:value` or plain tags joined with `;`        |

Both reporters can be enabled simultaneously.

---

## Extension DSL Reference

```kotlin
regression {
    // Directories searched in order for workflow files (.conf, .json, .yaml, .yml, .properties)
    workflowsDirs.add("src/test/resources/workflows")    // default

    // Directories searched in order for environment files
    environmentsDirs.add("src/test/resources/environments")  // default

    datasetProvider.set("local")    // local only today; S3 / Git are roadmap items
    datasetsRootDir.set("datasets") // default

    datasets {
        register("default") {
            path.set("datasets/default")
        }
    }

    reporting {
        allure {
            enabled.set(false)           // default
            version.set("2.27.0")        // default
            resultsDir.set("allure-results") // relative to build dir, default
        }
        reportPortal {
            enabled.set(false)           // default
            agentVersion.set("5.4.1")   // default
            endpoint.set("https://...")
            apiKey.set(providers.environmentVariable("RP_API_KEY"))
            project.set("my-project")
            launch.set("regression")
            description.set("...")
            attributes.set(listOf("env:prod", "suite:regression"))
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
WorkflowLoader          ← searches workflowsDirs for <name>.{conf,json,yaml,yml,properties}
   ↓
DatasetResolver         ← LocalDatasetProvider (or future S3)
   ↓
EnvResolver             ← searches environmentsDirs, merges base + <env> config
   ↓
KarateRunnerAdapter     ← builds CLI args
   ↓
ExecOperations.javaexec ← forks JVM, runs KarateJUnit5Launcher
   ↓
build/reports/regression/
```

### Config source priority (within a directory)

`.conf` → `.json` → `.yaml` → `.yml` → `.properties`

### Config source priority (across directories)

First directory in `workflowsDirs` / `environmentsDirs` wins.

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
