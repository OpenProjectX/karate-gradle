package org.openprojectx.karate.gradle.runner

import org.openprojectx.karate.model.Tags
import org.openprojectx.karate.model.Workflow

/** Assembled arguments for invoking `KarateJUnit5Launcher` via `javaexec`. */
data class KarateArgs(
    val systemProps: Map<String, Any>,
    val jvmArgs: List<String> = listOf("-Dfile.encoding=UTF-8"),
)

/**
 * Builds the [KarateArgs] needed to invoke `KarateJUnit5Launcher`.
 *
 * All configuration is passed as system properties to the forked JVM so that:
 * - `KarateRunner` reads feature paths and tag filters at runtime.
 * - Reporter agents (Allure, ReportPortal) read their own `rp.*` /
 *   `allure.results.directory` properties from the same JVM.
 *
 * This is a pure function — no Gradle API dependency so it can be unit-tested
 * without the Gradle runtime.
 */
object KarateRunnerAdapter {

    /** Main class generated into the consumer's test classpath by [GenerateKarateRunnerTask]. */
    const val LAUNCHER_MAIN_CLASS = "org.openprojectx.karate.runner.KarateJUnit5Launcher"

    fun buildArgs(
        workflow: Workflow,
        env: String,
        envConfig: Map<String, Any>,
        datasetPath: String,
        outputDir: String,
        commitHash: String? = null,
    ): KarateArgs {
        val sysProps = mutableMapOf<String, Any>(
            // Karate runtime config
            "karate.env"      to env,
            "karate.workflow" to workflow.name,
            "dataset.path"    to datasetPath,

            // KarateRunner reads these to build the Karate.run(...) call
            "karate.runner.features"     to workflow.features.joinToString(","),
            "karate.runner.tags.include" to workflow.tags.include.joinToString(","),
            "karate.runner.tags.exclude" to workflow.tags.exclude.joinToString(","),

            // JUnit Platform parallel execution — mirrors workflow.parallel
            "junit.jupiter.execution.parallel.enabled"                         to "true",
            "junit.jupiter.execution.parallel.mode.default"                    to "concurrent",
            "junit.jupiter.execution.parallel.config.strategy"                 to "fixed",
            "junit.jupiter.execution.parallel.config.fixed.parallelism"        to workflow.parallel.toString(),

            // Karate HTML report location
            "karate.output.dir" to outputDir,
        )

        envConfig.forEach { (key, value) ->
            sysProps["karate.config.$key"] = value.toString()
        }
        if (!commitHash.isNullOrBlank()) {
            sysProps["karate.commit"] = commitHash
        }

        return KarateArgs(systemProps = sysProps)
    }
}
