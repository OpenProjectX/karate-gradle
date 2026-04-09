Implement a gradle plugin(follow multi module design) for me following below design spec.

---

# 📄 Karate Regression Platform — Design Spec

## 1. Overview

### Goal

Build a **deterministic regression testing platform** on top of Karate with:

* commit-based reproducibility
* workflow orchestration (DSL)
* data versioning (contract vs runtime)
* environment abstraction
* Gradle-first execution (local + CI parity)

---

## 2. System Architecture

```text
+-----------------------------+
| Gradle Plugin (Control)     |
| - workflow DSL              |
| - execution engine          |
+-------------+---------------+
              |
              v
+-----------------------------+
| Karate Runtime (Execution)  |
+-------------+---------------+
              |
              v
+-----------------------------+
| APIs Under Test (Data Plane)|
+-----------------------------+
```

---

## 3. Core Concepts

### 3.1 Workflow

> Orchestration definition (NOT Karate-native)

Defines:

* features to run
* tags
* env
* dataset binding
* parallelism

---

### 3.2 Feature

* `.feature` files (Karate)
* represent API behavior
* composable units ONLY

---

### 3.3 Data

Two categories:

| Type          | Location | Versioning      |
| ------------- | -------- | --------------- |
| Contract Data | repo     | tied to commit  |
| Runtime Data  | external | dataset version |

---

### 3.4 Dataset

External data bundle:

```text
dataset = logical name → resolved path
```

Examples:

* `default`
* `incident-2026-04-09`
* `fuzz-v2`

---

### 3.5 Execution Key

```text
Execution = (commit, workflow, dataset, env)
```

---

## 4. Project Layout Specification

This is REQUIRED for plugin compatibility.

```text
repo-root/
├── build.gradle.kts
├── settings.gradle.kts
│
├── src/test/resources/
│   ├── features/
│   │   ├── user/
│   │   ├── payment/
│   │
│   ├── test-data/              # contract-bound
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
└── buildSrc/ or plugin/
    └── regression-plugin/
```

---

## 5. Workflow DSL Spec (YAML)

### Example

```yaml
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

---

### Replay Workflow

```yaml
name: replay

mode: replay

input: ${DATASET_PATH}

features:
  - classpath:features/replay/replay.feature

parallel: 1
```

---

## 6. Gradle Plugin Design

### Plugin ID

```text
org.openprojectx.karate.gradle
```

---

## 6.1 Extension DSL

```kotlin
regression {
    workflowsDir.set("src/test/resources/workflows")
    featuresDir.set("src/test/resources/features")
    testDataDir.set("src/test/resources/test-data")

    datasetProvider.set("local") // local | s3 | custom

    datasets {
        register("default") {
            path.set("datasets/default")
        }
        register("incident") {
            path.set("datasets/incidents")
        }
    }
}
```

---

## 6.2 CLI Parameters (Gradle)

```bash
./gradlew regressionRun \
  -Pworkflow=regression \
  -Penv=prod \
  -Pdataset=default \
  -Pcommit=abc123
```

---

## 6.3 Tasks

### Core Task

```text
regressionRun
```

---

### Task Responsibilities

1. resolve workflow YAML
2. resolve dataset path
3. load env config
4. construct Karate runtime args
5. execute tests
6. collect reports

---

## 6.4 Internal Execution Flow

```text
Gradle Task
   ↓
WorkflowLoader
   ↓
DatasetResolver
   ↓
EnvResolver
   ↓
KarateRunnerAdapter
   ↓
Execution
```

---

## 6.5 Kotlin Interfaces

### Workflow Model

```kotlin
data class Workflow(
    val name: String,
    val features: List<String>,
    val tags: Tags,
    val env: String,
    val dataset: String,
    val parallel: Int,
    val mode: String? = "standard"
)
```

---

### Dataset

```kotlin
data class Dataset(
    val name: String,
    val path: String
)
```

---

### Execution Context

```kotlin
data class ExecutionContext(
    val workflow: Workflow,
    val datasetPath: String,
    val envConfig: Map<String, Any>
)
```

---

## 7. Dataset Resolution

### Strategy Interface

```kotlin
interface DatasetProvider {
    fun resolve(dataset: String): Path
}
```

---

### Implementations

#### Local

```text
datasets/<dataset>/
```

---

#### S3 (future)

```text
s3://bucket/test-data/<dataset>/
```

---

---

## 8. Environment Resolution

### Merge Order

```text
base.yaml
   ↓
env.yaml
   ↓
runtime overrides
```

---

### Example

```yaml
# base.yaml
timeout: 5000

# prod.yaml
baseUrl: https://api.prod.com
```

---

---

## 9. Karate Integration

### Gradle → Karate

Pass via system properties:

```text
-Dkarate.env=prod
-Ddataset.path=/resolved/path
```

---

### Usage in feature

```gherkin
* def datasetPath = karate.properties['dataset.path']
* def input = read('file:' + datasetPath + '/case.json')
```

---

---

## 10. Incident Replay Model

### Structure

```text
datasets/incidents/
  case-123/
    request.json
    response.json
    metadata.yaml
```

---

### Replay Feature

```gherkin
Scenario: replay
  Given url baseUrl
  And request read(datasetPath + '/request.json')
  When method POST
  Then match response == read(datasetPath + '/response.json')
```

---

---

## 11. Reporting

### Output

```text
build/reports/regression/
```

---

### Optional (future)

* push to Elasticsearch
* compare results across commits

---

---

## 12. Jenkins Integration (Minimal)

Only pass parameters:

```groovy
sh """
./gradlew regressionRun \
  -Pworkflow=${params.WORKFLOW} \
  -Penv=${params.ENV} \
  -Pdataset=${params.DATASET}
"""
```

---

👉 No logic in Jenkins.

---

---

## 13. Local Development

### Run locally

```bash
./gradlew regressionRun \
  -Pworkflow=smoke \
  -Penv=staging
```

---

### Debug

* full logs in Gradle
* no CI dependency
* reproducible locally

---

---

## 14. Non-Goals

* ❌ No workflow logic inside `.feature`
* ❌ No environment hardcoding
* ❌ No Jenkins orchestration logic
* ❌ No mixing contract data with incident data

---

---

## 15. Future Extensions

* test impact analysis (commit diff → features)
* DSL in Kotlin instead of YAML
* integration with Kafka events (auto trigger)
* dataset version manifest + checksum
* Gradle test distribution

---

---

# 🚀 Final Summary

You are building:

> a **Gradle-native, workflow-driven, deterministic regression platform**

Key properties:

* reproducible: `(commit, dataset, workflow)`
* debuggable: local-first execution
* scalable: data + workflow separation
* extensible: plugin-based architecture

---

