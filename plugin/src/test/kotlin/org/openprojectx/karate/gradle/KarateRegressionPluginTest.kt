package org.openprojectx.karate.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFalse
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
                workflowsDirs.add("custom/workflows")
                environmentsDirs.add("custom/environments")
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

    @Test
    fun `forwards only prefixed user karate properties`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """rootProject.name = "test-consumer""""
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import org.openprojectx.karate.gradle.task.RegressionRunTask

            plugins {
                id("org.openprojectx.karate.gradle")
            }

            tasks.register("printKarateUserProps") {
                doLast {
                    val props = tasks.named<RegressionRunTask>("regressionRun").get().userKarateSystemProps.get()
                    println("karate.user.gradleProperty1=" + props["karate.user.gradleProperty1"])
                    println("karate.user.vmProperty1=" + props["karate.user.vmProperty1"])
                    println("karate.user.shared=" + props["karate.user.shared"])
                    println("gradleProperty1.present=" + props.containsKey("gradleProperty1"))
                    println("vmProperty1.present=" + props.containsKey("vmProperty1"))
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                "printKarateUserProps",
                "-Pkarate.user.gradleProperty1=test1",
                "-Pkarate.user.shared=fromGradle",
                "-PgradleProperty1=ignored",
                "-Dkarate.user.vmProperty1=vm1",
                "-Dkarate.user.shared=fromJvm",
                "-DvmProperty1=ignored",
            )
            .build()

        assertTrue(result.output.contains("karate.user.gradleProperty1=test1"))
        assertTrue(result.output.contains("karate.user.vmProperty1=vm1"))
        assertTrue(result.output.contains("karate.user.shared=fromJvm"))
        assertTrue(result.output.contains("gradleProperty1.present=false"))
        assertTrue(result.output.contains("vmProperty1.present=false"))
        assertFalse(result.output.contains("karate.user.shared=fromGradle"))
    }
}
