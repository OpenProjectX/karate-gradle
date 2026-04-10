package org.openprojectx.karate.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.openprojectx.karate.gradle.task.RegressionRunTask

/**
 * Plugin ID: `org.openprojectx.karate.gradle`
 *
 * Applies to a consumer project that runs Karate regression tests.
 * Registers:
 * - `regression { }` extension (workflow / dataset / env configuration)
 * - `regressionRun` task (CLI entry point)
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
 *     datasets {
 *         register("default") { path.set("datasets/default") }
 *     }
 * }
 * ```
 *
 * Running:
 * ```
 * ./gradlew regressionRun -Pworkflow=regression -Penv=prod -Pdataset=default
 * ```
 */
class KarateRegressionPlugin : Plugin<Project> {

    companion object {
        private const val DEFAULT_KARATE_VERSION = "1.5.2"
        private const val KARATE_ARTIFACT = "io.karatelabs:karate-junit5"
    }

    override fun apply(project: Project) {
        // Ensure java plugin is applied — provides SourceSetContainer + testRuntimeClasspath
        project.pluginManager.apply("java")

        // Auto-add karate-junit5 to consumer's testImplementation
        project.configurations.matching { it.name == "testImplementation" }.configureEach { config ->
            project.dependencies.add(config.name, "$KARATE_ARTIFACT:$DEFAULT_KARATE_VERSION")
        }

        // Create the regression { } extension
        val extension = project.extensions.create("regression", RegressionExtension::class.java)

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
}
