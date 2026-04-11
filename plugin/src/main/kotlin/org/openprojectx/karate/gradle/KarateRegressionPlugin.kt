package org.openprojectx.karate.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.openprojectx.karate.gradle.reporting.AllureConfig
import org.openprojectx.karate.gradle.reporting.ReportPortalConfig
import org.openprojectx.karate.gradle.task.GenerateKarateRunnerTask
import org.openprojectx.karate.gradle.task.RegressionRunTask
import org.openprojectx.karate.gradle.runner.KarateRunnerAdapter

/**
 * Plugin ID: `org.openprojectx.karate.gradle`
 *
 * Applies to a consumer project that runs Karate regression tests.
 * Registers:
 * - `regression { }` extension (workflow / dataset / env / reporting configuration)
 * - `regressionRun` task (CLI entry point)
 * - `generateKarateRunner` task (always — generates the JUnit5 entry point for `test`)
 *
 * Two execution paths:
 * ```
 * regressionRun  →  com.intuit.karate.Main  →  build/reports/regression/  (Karate HTML)
 * test           →  JUnit5 KarateRunner     →  Allure / ReportPortal agents (if enabled)
 * ```
 *
 * Consumer usage:
 * ```kotlin
 * plugins {
 *     id("org.openprojectx.karate.gradle")
 * }
 *
 * regression {
 *     workflowsDirs.add("src/test/resources/workflows")
 *     environmentsDirs.add("src/test/resources/environments")
 *
 *     datasets {
 *         register("default") { path.set("datasets/default") }
 *     }
 *
 *     reporting {
 *         allure {
 *             enabled.set(true)
 *         }
 *         reportPortal {
 *             enabled.set(true)
 *             endpoint.set("https://reportportal.example.com")
 *             apiKey.set(providers.environmentVariable("RP_API_KEY"))
 *             project.set("karate-regression")
 *             launch.set("smoke")
 *         }
 *     }
 * }
 * ```
 *
 * Running:
 * ```
 * ./gradlew regressionRun -Pworkflow=regression -Penv=prod -Pdataset=default
 * ./gradlew test                  # always runs features; reporters hook in automatically
 * ./gradlew test allureReport     # generates Allure HTML report (requires allure.enabled)
 * ```
 */
class KarateRegressionPlugin : Plugin<Project> {

    companion object {
        private const val DEFAULT_KARATE_VERSION = "1.5.2"
        private const val KARATE_ARTIFACT = "io.karatelabs:karate-junit5"
        private const val ALLURE_ARTIFACT = "io.qameta.allure:allure-junit5"
        private const val REPORT_PORTAL_ARTIFACT = "com.epam.reportportal:agent-java-junit5"
        // Must stay in sync with JUnit Jupiter version — Platform x.y.z matches Jupiter 5.y.z
        private const val JUNIT_PLATFORM_LAUNCHER = "org.junit.platform:junit-platform-launcher:1.11.4"
    }

    override fun apply(project: Project) {
        // Ensure java plugin is applied — provides SourceSetContainer + testRuntimeClasspath
        project.pluginManager.apply("java")

        // Auto-add karate-junit5 to consumer's testImplementation
        project.configurations.matching { it.name == "testImplementation" }.configureEach { config ->
            project.dependencies.add(config.name, "$KARATE_ARTIFACT:$DEFAULT_KARATE_VERSION")
        }

        // junit-platform-launcher must be on the compile + runtime classpath:
        //  - compile: KarateJUnit5Launcher.java imports JUnit Platform Launcher API
        //  - runtime: LauncherFactory.create() discovers TestExecutionListener SPI impls
        project.dependencies.add("testImplementation", JUNIT_PLATFORM_LAUNCHER)

        // The standard `test` Gradle task is not the entry point — regressionRun is.
        // Suppress the "no tests discovered" error so `./gradlew check` doesn't fail.
        project.tasks.withType(Test::class.java).configureEach { test ->
            test.failOnNoDiscoveredTests.set(false)
        }

        // Create the regression { } extension
        val extension = project.extensions.create("regression", RegressionExtension::class.java)

        // Generate the JUnit5 runner sources that regressionRun forks.
        // Unconditional — reporter agents hook in automatically when their jars
        // are on the classpath, no code change needed to toggle them on/off.
        registerKarateRunnerGeneration(project, extension)

        // Inject reporting dependencies after the build script has been evaluated
        // so the reporting { } block has been fully configured before we read it.
        project.afterEvaluate {
            addReportingDependencies(project, extension)
        }

        // Register regressionRun task (lazy — no eager configuration)
        project.tasks.register("regressionRun", RegressionRunTask::class.java) { task ->
            task.group       = "regression"
            task.description = "Run Karate regression tests according to a workflow definition"

            val layout    = project.layout
            val providers = project.providers

            // Wire multi-source directories (in order: first dir has highest priority)
            task.workflowsDirs.from(
                extension.workflowsDirs.map { dirs ->
                    dirs.map { layout.projectDirectory.dir(it) }
                }
            )
            task.environmentsDirs.from(
                extension.environmentsDirs.map { dirs ->
                    dirs.map { layout.projectDirectory.dir(it) }
                }
            )

            // Datasets root as absolute string (serializable for config cache)
            task.datasetsRootDir.set(
                extension.datasetsRootDir.map { layout.projectDirectory.dir(it).asFile.absolutePath }
            )
            task.datasetProviderType.set(extension.datasetProvider)

            // Wire dataset registry from the NamedDomainObjectContainer → MapProperty<String, String>
            task.datasetRegistry.set(
                providers.provider {
                    extension.datasets.associate { spec -> spec.name to spec.path.get() }
                }
            )

            // CLI parameters (lazy providers — evaluated at execution time, config-cache safe)
            task.workflowName.set(providers.gradleProperty("workflow").orElse("regression"))
            task.envName.set(providers.gradleProperty("env"))
            task.datasetName.set(providers.gradleProperty("dataset"))
            task.commitHash.set(providers.gradleProperty("commit"))

            // Reporting system properties (Allure + ReportPortal)
            task.reportingSystemProps.set(
                providers.provider {
                    buildReportingProps(extension, layout)
                }
            )

            // Wire test classpath from the test source set
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            val testSourceSet = sourceSets.getByName("test")
            task.testClasspath.from(
                testSourceSet.runtimeClasspath,
                testSourceSet.output.classesDirs,
                testSourceSet.output.resourcesDir,
            )

            // Output
            task.reportsDir.set(layout.buildDirectory.dir("reports/regression"))

            // Ensure test classes are compiled before running
            task.dependsOn(project.tasks.named("testClasses"))
        }
    }

    /**
     * Registers [GenerateKarateRunnerTask] and wires its output into the `test`
     * Java source set so that [RegressionRunTask] can find `KarateJUnit5Launcher`
     * and `KarateRunner` on the test runtime classpath.
     */
    private fun registerKarateRunnerGeneration(project: Project, extension: RegressionExtension) {
        val outputDir = project.layout.buildDirectory.dir("generated-test-sources/karate-gradle")

        val generateTask = project.tasks.register(
            "generateKarateRunner",
            GenerateKarateRunnerTask::class.java,
        ) { task ->
            task.group       = "regression"
            task.description = "Generates KarateRunner and KarateJUnit5Launcher for regressionRun"
            task.outputDir.set(outputDir)
        }

        // Add generated sources to the test Java source set so they compile with testClasses
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        sourceSets.named("test") { testSourceSet ->
            testSourceSet.java.srcDir(outputDir)
        }

        // Ensure the runner is generated before test compilation
        project.tasks.withType(JavaCompile::class.java).configureEach { compile ->
            if (compile.name == "compileTestJava") {
                compile.dependsOn(generateTask)
            }
        }
    }

    private fun addReportingDependencies(project: Project, extension: RegressionExtension) {
        val allure = extension.reporting.allure
        if (allure.enabled.getOrElse(false)) {
            project.dependencies.add(
                "testImplementation",
                "$ALLURE_ARTIFACT:${allure.version.get()}",
            )
            // Wire the results directory on every JUnit5 Test task so that
            // `./gradlew test allureReport` picks up the correct location.
            val resultsDir = project.layout.buildDirectory
                .dir(allure.resultsDir.get()).get().asFile.absolutePath
            project.tasks.withType(Test::class.java).configureEach { test ->
                test.systemProperty("allure.results.directory", resultsDir)
            }
        }

        val rp = extension.reporting.reportPortal
        if (rp.enabled.getOrElse(false)) {
            project.dependencies.add(
                "testImplementation",
                "$REPORT_PORTAL_ARTIFACT:${rp.agentVersion.get()}",
            )
            // ReportPortal agent reads rp.* properties from the JVM it runs in.
            // Wire them onto every JUnit5 Test task so `./gradlew test` streams
            // results to the configured RP server automatically.
            project.tasks.withType(Test::class.java).configureEach { test ->
                rp.endpoint.orNull?.let    { test.systemProperty("rp.endpoint",    it) }
                rp.apiKey.orNull?.let      { test.systemProperty("rp.api.key",     it) }
                rp.project.orNull?.let     { test.systemProperty("rp.project",     it) }
                rp.launch.orNull?.let      { test.systemProperty("rp.launch",      it) }
                rp.description.orNull?.let { test.systemProperty("rp.description", it) }
                val attrs = rp.attributes.getOrElse(emptyList())
                if (attrs.isNotEmpty()) test.systemProperty("rp.attributes", attrs.joinToString(";"))
            }
        }
    }

    private fun buildReportingProps(
        extension: RegressionExtension,
        layout: org.gradle.api.file.ProjectLayout,
    ): Map<String, String> = buildMap {
        addAllureProps(extension.reporting.allure, layout)
        addReportPortalProps(extension.reporting.reportPortal)
    }

    private fun MutableMap<String, String>.addAllureProps(
        allure: AllureConfig,
        layout: org.gradle.api.file.ProjectLayout,
    ) {
        if (!allure.enabled.getOrElse(false)) return
        put(
            "allure.results.directory",
            layout.buildDirectory.dir(allure.resultsDir.get()).get().asFile.absolutePath,
        )
    }

    private fun MutableMap<String, String>.addReportPortalProps(rp: ReportPortalConfig) {
        if (!rp.enabled.getOrElse(false)) return
        rp.endpoint.orNull?.let    { put("rp.endpoint",     it) }
        rp.apiKey.orNull?.let      { put("rp.api.key",      it) }
        rp.project.orNull?.let     { put("rp.project",      it) }
        rp.launch.orNull?.let      { put("rp.launch",       it) }
        rp.description.orNull?.let { put("rp.description",  it) }
        val attrs = rp.attributes.getOrElse(emptyList())
        if (attrs.isNotEmpty()) put("rp.attributes", attrs.joinToString(";"))
    }
}
