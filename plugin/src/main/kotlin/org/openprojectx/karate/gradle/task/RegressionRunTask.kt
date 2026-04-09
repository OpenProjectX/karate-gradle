package org.openprojectx.karate.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.openprojectx.karate.provider.LocalDatasetProvider
import org.openprojectx.karate.service.DatasetResolver
import org.openprojectx.karate.service.EnvResolver
import org.openprojectx.karate.service.WorkflowLoader
import org.openprojectx.karate.gradle.runner.KarateRunnerAdapter
import javax.inject.Inject

/**
 * Runs Karate regression tests according to a named workflow definition.
 *
 * Execution key: `(commit, workflow, dataset, env)` — all four are inputs that
 * guarantee deterministic, reproducible runs.
 *
 * Usage:
 * ```
 * ./gradlew regressionRun -Pworkflow=regression -Penv=prod -Pdataset=default -Pcommit=abc123
 * ```
 */
abstract class RegressionRunTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    // ── Wired from RegressionExtension at configuration time ──────────────────

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val workflowsDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val environmentsDir: DirectoryProperty

    /** Absolute path of the local datasets root directory. */
    @get:Input
    abstract val datasetsRootDir: Property<String>

    /** "local" | future: "s3" */
    @get:Input
    abstract val datasetProviderType: Property<String>

    /** Registered datasets: name → relative path under datasetsRootDir. */
    @get:Input
    abstract val datasetRegistry: MapProperty<String, String>

    // ── CLI parameters (-Pworkflow=, -Penv=, -Pdataset=, -Pcommit=) ──────────

    @get:Input
    abstract val workflowName: Property<String>

    @get:Input
    @get:Optional
    abstract val envName: Property<String>

    @get:Input
    @get:Optional
    abstract val datasetName: Property<String>

    @get:Input
    @get:Optional
    abstract val commitHash: Property<String>

    // ── Consumer project's test classpath ─────────────────────────────────────

    @get:InputFiles
    @get:Classpath
    abstract val testClasspath: ConfigurableFileCollection

    // ── Output ────────────────────────────────────────────────────────────────

    @get:OutputDirectory
    abstract val reportsDir: DirectoryProperty

    // ─────────────────────────────────────────────────────────────────────────

    @TaskAction
    fun run() {
        val workflowsDirFile    = workflowsDir.get().asFile
        val environmentsDirFile = environmentsDir.orNull?.asFile
        val reportsOutput       = reportsDir.get().asFile.also { it.mkdirs() }

        // 1. Load workflow YAML
        val workflow = WorkflowLoader(workflowsDirFile).load(workflowName.get())

        // 2. Effective env: CLI override → workflow YAML → "base"
        val effectiveEnv = envName.orNull?.takeIf { it.isNotBlank() } ?: workflow.env

        // 3. Resolve env config (base + env merge)
        val envConfig = if (environmentsDirFile != null && environmentsDirFile.exists()) {
            EnvResolver(environmentsDirFile).resolve(effectiveEnv)
        } else {
            emptyMap()
        }

        // 4. Resolve dataset path
        val effectiveDataset = datasetName.orNull?.takeIf { it.isNotBlank() } ?: workflow.dataset
        val registry = datasetRegistry.get()
        val rootDir  = java.io.File(datasetsRootDir.get())
        val provider = LocalDatasetProvider(rootDir, registry)
        val datasetPath = DatasetResolver(provider).resolve(effectiveDataset).toString()

        // 5. Build Karate invocation args
        val karateArgs = KarateRunnerAdapter.buildArgs(
            workflow    = workflow,
            env         = effectiveEnv,
            envConfig   = envConfig,
            datasetPath = datasetPath,
            outputDir   = reportsOutput.absolutePath,
            commitHash  = commitHash.orNull
        )

        logger.lifecycle("Executing workflow '${workflow.name}' | env=$effectiveEnv | dataset=$effectiveDataset | threads=${workflow.parallel}")
        logger.info("Karate args: ${karateArgs.positionalArgs}")
        logger.info("System properties: ${karateArgs.systemProps}")

        // 6. Fork JVM and run com.intuit.karate.Main (ExecOperations is config-cache safe)
        execOperations.javaexec { spec ->
            spec.classpath(testClasspath)
            spec.mainClass.set(KarateRunnerAdapter.KARATE_MAIN_CLASS)
            spec.args(karateArgs.positionalArgs)
            spec.systemProperties(karateArgs.systemProps)
            spec.jvmArgs(karateArgs.jvmArgs)
        }
    }
}
