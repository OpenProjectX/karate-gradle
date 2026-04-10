package org.openprojectx.karate.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WorkflowLoaderTest {

    @TempDir
    lateinit var workflowsDir: File

    @Test
    fun `loads full workflow from hocon`() {
        workflowsDir.resolve("regression.conf").writeText(
            """
            name = regression
            features = [
              "classpath:features/user/**"
              "classpath:features/payment/**"
            ]
            tags {
              "include" = ["@regression"]
              exclude = ["@ignore"]
            }
            env = prod
            dataset = default
            parallel = 10
            """.trimIndent()
        )

        val workflow = WorkflowLoader(workflowsDir).load("regression")

        assertEquals("regression", workflow.name)
        assertEquals(listOf("classpath:features/user/**", "classpath:features/payment/**"), workflow.features)
        assertEquals(listOf("@regression"), workflow.tags.include)
        assertEquals(listOf("@ignore"), workflow.tags.exclude)
        assertEquals("prod", workflow.env)
        assertEquals("default", workflow.dataset)
        assertEquals(10, workflow.parallel)
        assertEquals("standard", workflow.mode)
    }

    @Test
    fun `applies defaults for missing optional fields`() {
        workflowsDir.resolve("smoke.conf").writeText(
            """
            name = smoke
            features = []
            """.trimIndent()
        )

        val workflow = WorkflowLoader(workflowsDir).load("smoke")

        assertEquals("base", workflow.env)
        assertEquals("default", workflow.dataset)
        assertEquals(1, workflow.parallel)
        assertEquals("standard", workflow.mode)
    }

    @Test
    fun `loads workflow from json source`() {
        workflowsDir.resolve("contract.json").writeText(
            """
            {
              "name": "contract",
              "features": ["classpath:features/api/**"],
              "env": "staging",
              "parallel": 2
            }
            """.trimIndent()
        )

        val workflow = WorkflowLoader(workflowsDir).load("contract")

        assertEquals("contract", workflow.name)
        assertEquals("staging", workflow.env)
        assertEquals(2, workflow.parallel)
    }

    @Test
    fun `merges workflow from multiple source directories`() {
        val overrideDir = workflowsDir.resolve("override").also { it.mkdirs() }

        workflowsDir.resolve("shared.conf").writeText(
            """
            name = shared
            features = ["classpath:features/base/**"]
            env = staging
            parallel = 4
            """.trimIndent()
        )
        overrideDir.resolve("shared.conf").writeText(
            """
            name = shared
            env = prod
            """.trimIndent()
        )

        val loader = WorkflowLoader(
            listOf(
                org.openprojectx.karate.config.ConfigSource.LocalDirectory(overrideDir),
                org.openprojectx.karate.config.ConfigSource.LocalDirectory(workflowsDir),
            )
        )
        val workflow = loader.load("shared")

        assertEquals("prod", workflow.env)                              // overrideDir wins
        assertEquals(listOf("classpath:features/base/**"), workflow.features)  // from base dir
        assertEquals(4, workflow.parallel)                              // from base dir
    }

    @Test
    fun `throws when workflow is not found in any source`() {
        assertFailsWith<IllegalArgumentException> {
            WorkflowLoader(workflowsDir).load("nonexistent")
        }
    }
}
