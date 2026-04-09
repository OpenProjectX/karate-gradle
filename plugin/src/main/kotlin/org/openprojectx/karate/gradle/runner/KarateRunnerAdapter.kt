package org.openprojectx.karate.gradle.runner

import org.openprojectx.karate.model.Tags
import org.openprojectx.karate.model.Workflow

/** Assembled arguments for invoking `com.intuit.karate.Main` via `javaexec`. */
data class KarateArgs(
    val positionalArgs: List<String>,
    val systemProps: Map<String, Any>,
    val jvmArgs: List<String> = listOf("-Dfile.encoding=UTF-8")
)

/**
 * Builds the [KarateArgs] needed to invoke `com.intuit.karate.Main`.
 *
 * This is a pure function — no Gradle API dependency so it can be unit-tested without
 * the Gradle runtime.
 */
object KarateRunnerAdapter {

    /** The fully-qualified main class in `io.karatelabs:karate-core`. */
    const val KARATE_MAIN_CLASS = "com.intuit.karate.Main"

    fun buildArgs(
        workflow: Workflow,
        env: String,
        envConfig: Map<String, Any>,
        datasetPath: String,
        outputDir: String,
        commitHash: String? = null
    ): KarateArgs {
        val positional = mutableListOf<String>()

        // Feature paths (classpath: or file-system paths)
        positional.addAll(workflow.features)

        // Tag filter (Karate CLI: --tags @tag1,~@excluded)
        val tagArg = buildTagArg(workflow.tags)
        if (tagArg.isNotBlank()) {
            positional += listOf("--tags", tagArg)
        }

        // Parallel threads
        positional += listOf("--threads", workflow.parallel.toString())

        // Output directory
        positional += listOf("--output", outputDir)

        // System properties forwarded to Karate
        val sysProps = mutableMapOf<String, Any>(
            "karate.env" to env,
            "karate.workflow" to workflow.name,
            "dataset.path" to datasetPath
        )
        envConfig.forEach { (key, value) ->
            sysProps["karate.config.$key"] = value.toString()
        }
        if (!commitHash.isNullOrBlank()) {
            sysProps["karate.commit"] = commitHash
        }

        return KarateArgs(
            positionalArgs = positional,
            systemProps    = sysProps
        )
    }

    private fun buildTagArg(tags: Tags): String {
        val parts = tags.include + tags.exclude.map { "~$it" }
        return parts.joinToString(",")
    }
}
