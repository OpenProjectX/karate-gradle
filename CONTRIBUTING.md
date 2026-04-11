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

## Running the Example (End-to-End)

`example/` is a standalone consumer project that applies the plugin via composite build —
no `publishToMavenLocal` required. It runs real Karate tests against
[jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com).

```bash
cd example

# Smoke suite (fast, @smoke tagged scenarios only)
../gradlew regressionRun -Pworkflow=smoke -Penv=staging

# Full regression suite
../gradlew regressionRun -Pworkflow=regression -Penv=staging

# With a specific dataset
../gradlew regressionRun -Pworkflow=regression -Penv=staging -Pdataset=default
```

Reports land in `example/build/reports/regression/karate-reports/karate-summary.html`.

The `example/` directory is excluded from the root multi-project build. It has its own
`settings.gradle.kts` with `includeBuild("..")` that pulls the plugin directly from source.

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

## Config File Format

Workflow and environment files support multiple formats. The discovery order within a directory is:

`.conf` (HOCON) → `.json` → `.yaml` → `.yml` → `.properties`

HOCON is the preferred format for new files. YAML is fully supported and requires no migration.

### HOCON gotcha: `include` is a reserved keyword

Inside a `tags {}` block, quote the `include` key:

```hocon
tags {
  "include" = ["@smoke"]   # must be quoted in HOCON
  exclude   = ["@ignore"]  # no issue
}
```

YAML files are not affected — `include:` is a regular key in YAML.

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
2. Update `WorkflowLoader.parseWorkflow()` to read it from the `Config` object:
   ```kotlin
   myField = if (config.hasPath("myField")) config.getString("myField") else "default"
   ```
3. Use it in `KarateRunnerAdapter.buildArgs()` or `RegressionRunTask.run()`.
4. Add a test case in `WorkflowLoaderTest` — write a `.conf` or `.yaml` temp file, then assert the field.

---

## Adding a New Config Source Type

`ConfigSource` is a sealed class in `core/src/main/kotlin/org/openprojectx/karate/config/ConfigSource.kt`.
To add a new source (e.g. remote config server):

1. Add a new subclass:

```kotlin
class RemoteUrl(val url: String) : ConfigSource() {
    override fun forName(name: String): Config {
        // fetch and parse
    }
}
```

2. Wire it from the extension/task if it needs to be user-configurable.

---

## Commit Messages

Follow conventional commits:

```
feat: add S3 dataset provider
fix: handle missing base config gracefully
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

## Documentation

User documentation is authored in AsciiDoc under `doc/`.

### Build docs locally

```bash
./gradlew asciidoctor
```

Generated HTML lands in:

```text
build/docs/
```

The main user guide currently lives at:

```text
doc/user-guide.adoc
```

### GitHub Pages publishing model

This repository is configured to publish documentation to the repository site via
GitHub Pages and GitHub Actions.

Workflow:

```text
doc/*.adoc -> ./gradlew asciidoctor -> build/docs -> GitHub Pages artifact -> repository site
```

Important clarification:

- This setup publishes to the repository site
- It does **not** maintain or push a `gh-pages` branch
- GitHub Pages should be configured in repository settings to use `GitHub Actions` as the source

This means the docs site is served by GitHub Pages, but deployment is driven by the Actions
workflow in `.github/workflows/publish-docs.yml` rather than by committing generated files to
a branch.

### Documentation workflow

The docs publishing workflow is:

```text
.github/workflows/publish-docs.yml
```

It runs when:

- code or doc changes affecting documentation are pushed to `master`
- manually triggered via `workflow_dispatch`

What it does:

1. Checks out the repository
2. Sets up JDK 17
3. Runs `./gradlew asciidoctor`
4. Uploads `build/docs` as a GitHub Pages artifact
5. Deploys the artifact to the repository Pages site

### Configuration-cache note

The Asciidoctor task currently generates HTML successfully, but it is not configuration-cache
compatible in this build. This does not block doc generation or Pages deployment.

---

## Reporting Issues

Open an issue at [github.com/OpenProjectX/karate-gradle/issues](https://github.com/OpenProjectX/karate-gradle/issues) with:

- Gradle version (`./gradlew --version`)
- JDK version (`java -version`)
- Minimal reproduction (workflow file + `build.gradle.kts` snippet)
- Full error output

---

## Publishing to Maven Local (Pre-release Inspection)

Before releasing to Maven Central, publish to your local Maven repository and verify
the artifacts look correct. This is a required step before any release.

### 1. Publish all modules

```bash
./gradlew publishToMavenLocal
```

This publishes to `~/.m2/repository/org/openprojectx/karate/gradle/`.

### 2. What gets published

| Artifact | Coordinates | Description |
|---|---|---|
| Plugin jar | `org.openprojectx.karate.gradle:plugin:<version>` | The compiled plugin |
| Plugin marker | `org.openprojectx.karate.gradle:org.openprojectx.karate.gradle.gradle.plugin:<version>` | Marker POM that maps plugin ID → coordinates |
| Core library | `org.openprojectx.karate.gradle:core:<version>` | Models and services |

### 3. Inspect the POMs

```bash
# Plugin POM — verify name, description, url, license, scm, developers are present
cat ~/.m2/repository/org/openprojectx/karate/gradle/plugin/<version>/plugin-<version>.pom

# Marker POM
cat ~/.m2/repository/org/openprojectx/karate/gradle/org.openprojectx.karate.gradle.gradle.plugin/<version>/*.pom

# Core POM
cat ~/.m2/repository/org/openprojectx/karate/gradle/core/<version>/core-<version>.pom
```

All six Maven Central required fields must be present in every POM:
`<name>`, `<description>`, `<url>`, `<licenses>`, `<developers>`, `<scm>`.

### 4. Consume from a test project

Create a minimal consumer project to verify the plugin loads and the `regressionRun` task registers:

```kotlin
// consumer/settings.gradle.kts
pluginManagement {
    repositories {
        mavenLocal()          // pick up the locally published plugin
        gradlePluginPortal()
    }
}
rootProject.name = "consumer-test"
```

```kotlin
// consumer/build.gradle.kts
plugins {
    id("org.openprojectx.karate.gradle") version "0.2.0-SNAPSHOT"
}

regression {
    workflowsDirs.add("src/test/resources/workflows")
}
```

```bash
cd consumer
./gradlew tasks --group=regression
# Expected: regressionRun — Run Karate regression tests according to a workflow definition
```

### 5. Verify the plugin marker resolves correctly

Gradle resolves plugin IDs via the marker POM. Confirm resolution works end-to-end:

```bash
cd consumer
./gradlew dependencies --configuration=classpath 2>&1 | grep karate
# Expected: org.openprojectx.karate.gradle:plugin:<version>
```

---

## Release Process

Releases are managed via `net.researchgate.release` and published to Maven Central through Sonatype.

### GitHub Actions release workflow

Maintainers can trigger a manual release through:

```text
.github/workflows/release.yml
```

This workflow:

1. Checks out `master` with full git history
2. Sets up JDK 17
3. Configures a Git identity for the workflow run
4. Materializes the signing key from a GitHub secret into a temporary file
5. Runs `./gradlew release`

Trigger model:

- automatically on every push to `master`
- manually through `workflow_dispatch`

The manual trigger accepts an optional `gradle_args` input for extra release flags such as
`--stacktrace`.

The release workflow is intentionally separate from documentation publishing.

Reason:

- a documentation deployment failure should not block a Maven Central release
- docs can be updated independently of artifact publishing
- the release commit pushed back to `master` can still trigger the docs workflow naturally

In short:

- release pipeline: artifact publishing and tagging
- docs pipeline: site publishing

Do not couple Pages deployment into the release workflow unless you have a hard requirement to
make artifact release and site update succeed or fail as a single unit.

### Required environment variables

```bash
export SIGNING_KEY_FILE=/path/to/private.key   # ASCII-armored GPG private key
export SIGNING_KEY_PASSWORD=<passphrase>
export OSSRH_USERNAME=<sonatype-token-username>
export OSSRH_PASSWORD=<sonatype-token-password>
```

### Required GitHub secrets for the release workflow

The GitHub Actions release workflow expects:

```text
RELEASE_GITHUB_TOKEN
SIGNING_KEY_ASC
SIGNING_KEY_PASSWORD
OSSRH_USERNAME
OSSRH_PASSWORD
```

Notes:

- `RELEASE_GITHUB_TOKEN` is used by `actions/checkout` so the workflow can push release commits and tags
- `SIGNING_KEY_ASC` should contain the ASCII-armored private key content itself
- the workflow writes that secret to a temporary file and exports `SIGNING_KEY_FILE` for Gradle
- `OSSRH_USERNAME` and `OSSRH_PASSWORD` should be Sonatype token credentials

### Steps

**1. Inspect locally first (see above)**

```bash
./gradlew publishToMavenLocal
```

**2. Run all tests**

```bash
./gradlew test
```

**3. Release**

```bash
# Maintainers only — bumps version, tags, publishes to Sonatype, closes and releases staging repo
./gradlew release
```

This triggers: `publishToSonatype` → `closeAndReleaseSonatypeStagingRepository`.

### What is published

Three publication tasks fire during release:

| Task | Artifact |
|---|---|
| `publishPluginMavenPublicationToSonatypeRepository` | Plugin jar + sources + javadoc |
| `publishKarateRegressionPluginMarkerMavenPublicationToSonatypeRepository` | Plugin marker POM |
| `publishMavenJavaPublicationToSonatypeRepository` (`:core` only) | Core jar + sources + javadoc |

`publishMavenJavaPublicationToSonatypeRepository` in `:plugin` is intentionally disabled —
`pluginMaven` is the canonical publication for the plugin module.
