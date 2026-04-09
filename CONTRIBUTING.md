# Contributing

Thank you for your interest in contributing to karate-gradle.

---

## Project Structure

```
karate-gradle/
├── core/        # Pure Kotlin library — models, services, providers (no Gradle API)
├── plugin/      # Gradle plugin — extension DSL, tasks, Karate adapter
└── buildSrc/    # kotlin-jvm convention plugin shared across modules
```

**Rule:** `core` must never depend on Gradle API. Keep all Gradle-specific code in `plugin`.

---

## Prerequisites

- JDK 17+
- Gradle wrapper is included — no local Gradle installation needed

```bash
./gradlew --version
```

---

## Building

```bash
# Build all modules
./gradlew :core:build :plugin:build

# Build a single module
./gradlew :core:build
./gradlew :plugin:build
```

---

## Testing

```bash
# Run all tests
./gradlew test

# Core unit tests (WorkflowLoader, EnvResolver, etc.)
./gradlew :core:test

# Plugin integration tests (Gradle TestKit)
./gradlew :plugin:test
```

Test reports: `<module>/build/reports/tests/test/index.html`

### What to test

| Layer | Test type | Location |
|---|---|---|
| `core` services | Unit tests with `@TempDir` | `core/src/test/kotlin/` |
| Plugin wiring | Gradle TestKit (`GradleRunner`) | `plugin/src/test/kotlin/` |
| `KarateRunnerAdapter` | Unit tests (pure function) | `plugin/src/test/kotlin/` |

---

## Code Style

- Kotlin official code style (`kotlin.code.style=official` in `gradle.properties`)
- No wildcard imports
- No unnecessary abstraction — solve the actual problem, not a hypothetical one
- New public API needs a KDoc comment explaining purpose, not mechanics

---

## Adding a New Module

The `settings.gradle.kts` auto-discovers modules by scanning for `build.gradle.kts` files. To add a new module:

1. Create `<module-name>/build.gradle.kts`
2. Apply the convention plugin: `id("buildsrc.convention.kotlin-jvm")`
3. Run `./gradlew projects` to confirm it is discovered

```kotlin
// <module-name>/build.gradle.kts
plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":core"))
}
```

---

## Adding a New Dataset Provider

1. Implement `DatasetProvider` in `core`:

```kotlin
// core/src/main/kotlin/org/openprojectx/karate/provider/S3DatasetProvider.kt
class S3DatasetProvider(private val bucket: String) : DatasetProvider {
    override fun resolve(datasetName: String): Path {
        // download from S3, return local path
    }
}
```

2. Wire it in `RegressionRunTask.run()` inside the `when (datasetProviderType.get())` block:

```kotlin
"s3" -> S3DatasetProvider(...)
```

3. Add any new extension properties to `RegressionExtension` for configuration.

---

## Adding a New Workflow Field

1. Add the field to `Workflow` in `core/src/main/kotlin/org/openprojectx/karate/model/Models.kt` with a default value.
2. Update `WorkflowLoader.mapToWorkflow()` to read it from the YAML map.
3. Use it in `KarateRunnerAdapter.buildArgs()` or `RegressionRunTask.run()`.
4. Add a test case in `WorkflowLoaderTest`.

---

## Commit Messages

Follow conventional commits:

```
feat: add S3 dataset provider
fix: handle missing base.yaml gracefully
test: add EnvResolver edge case for empty env file
refactor: extract tag arg builder to KarateRunnerAdapter
```

Use `feat` for new features, `fix` for bug fixes, `test` for test-only changes, `refactor` for internal cleanup.

---

## Pull Requests

- One logical change per PR
- All tests must pass: `./gradlew test`
- Include a short description of **why**, not just what changed
- Reference any related issue: `Closes #123`

---

## Reporting Issues

Open an issue at [github.com/OpenProjectX/karate-gradle/issues](https://github.com/OpenProjectX/karate-gradle/issues) with:

- Gradle version (`./gradlew --version`)
- JDK version (`java -version`)
- Minimal reproduction (workflow YAML + `build.gradle.kts` snippet)
- Full error output

---

## Release Process

Releases are managed via `net.researchgate.release` and published to Maven Central through Sonatype.

```bash
# Maintainers only
./gradlew release
```

This triggers: `publishToSonatype` → `closeAndReleaseSonatypeStagingRepository`.

Artifacts are signed using a GPG key provided via `SIGNING_KEY_FILE` / `SIGNING_KEY_PASSWORD` environment variables.
