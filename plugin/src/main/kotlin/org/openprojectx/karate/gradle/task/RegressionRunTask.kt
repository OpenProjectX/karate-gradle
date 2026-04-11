package org.openprojectx.karate.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.openprojectx.karate.config.ConfigSource
import org.openprojectx.karate.gradle.runner.KarateRunnerAdapter
import org.openprojectx.karate.provider.LocalDatasetProvider
import org.openprojectx.karate.service.DatasetResolver
import org.openprojectx.karate.service.EnvResolver
import org.openprojectx.karate.service.WorkflowLoader
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
    private val execOperations: ExecOperations,
    objects: ObjectFactory,
) : DefaultTask() {

    // ── Wired from RegressionExtension at configuration time ──────────────────

    /** Directories searched (in order) for workflow config files. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val workflowsDirs: ConfigurableFileCollection = objects.fileCollection()

    /** Directories searched (in order) for environment config files. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val environmentsDirs: ConfigurableFileCollection = objects.fileCollection()

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

    // ── Reporting (Allure / ReportPortal) system properties ───────────────────

    /**
     * System properties collected from enabled reporters.
     * Merged with Karate's own system properties before forking the JVM.
     */
    @get:Input
    abstract val reportingSystemProps: MapProperty<String, String>

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
        val reportsOutput = reportsDir.get().asFile.also { it.mkdirs() }

        val workflowSources = workflowsDirs.files.map { ConfigSource.LocalDirectory(it) }
        val envSources      = environmentsDirs.files.map { ConfigSource.LocalDirectory(it) }

        val workflow = WorkflowLoader(workflowSources).load(workflowName.get())

        val effectiveEnv = envName.orNull?.takeIf { it.isNotBlank() } ?: workflow.env

        val envConfig = if (envSources.isNotEmpty()) {
            EnvResolver(envSources).resolve(effectiveEnv)
        } else {
            emptyMap()
        }

        val effectiveDataset = datasetName.orNull?.takeIf { it.isNotBlank() } ?: workflow.dataset
        val provider = LocalDatasetProvider(
            datasetsRootDir = java.io.File(datasetsRootDir.get()),
            datasetPaths    = datasetRegistry.get(),
        )
        val datasetPath = DatasetResolver(provider).resolve(effectiveDataset).toString()

        val karateArgs = KarateRunnerAdapter.buildArgs(
            workflow    = workflow,
            env         = effectiveEnv,
            envConfig   = envConfig,
            datasetPath = datasetPath,
            outputDir   = reportsOutput.absolutePath,
            commitHash  = commitHash.orNull,
        )

        logger.lifecycle("Executing workflow '${workflow.name}' | env=$effectiveEnv | dataset=$effectiveDataset | threads=${workflow.parallel}")
        logger.info("System properties: ${karateArgs.systemProps}")

        val allSystemProps = karateArgs.systemProps + reportingSystemProps.getOrElse(emptyMap())

        execOperations.javaexec { spec ->
            spec.classpath(testClasspath)
            spec.mainClass.set(KarateRunnerAdapter.LAUNCHER_MAIN_CLASS)
            spec.systemProperties(allSystemProps)
            spec.jvmArgs(karateArgs.jvmArgs)
        }
    }
}
