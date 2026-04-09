package org.openprojectx.karate.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class KarateRegressionPluginTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `plugin registers regressionRun task`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """rootProject.name = "test-consumer""""
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.openprojectx.karate.gradle")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group=regression", "--stacktrace")
            .build()

        assertTrue(result.output.contains("regressionRun"), "Expected regressionRun task in output:\n${result.output}")
    }

    @Test
    fun `extension DSL is configurable without error`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """rootProject.name = "test-consumer""""
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.openprojectx.karate.gradle")
            }
            regression {
                workflowsDir.set("custom/workflows")
                datasetProvider.set("local")
                datasets {
                    register("default") { path.set("datasets/default") }
                    register("incident") { path.set("datasets/incidents") }
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group=regression")
            .build()

        assertTrue(result.output.contains("regressionRun"))
    }
}
