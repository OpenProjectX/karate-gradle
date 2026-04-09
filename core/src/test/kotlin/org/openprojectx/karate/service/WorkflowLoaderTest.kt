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
    fun `loads full workflow from yaml`() {
        workflowsDir.resolve("regression.yaml").writeText(
            """
            name: regression
            features:
              - classpath:features/user/**
              - classpath:features/payment/**
            tags:
              include: ["@regression"]
              exclude: ["@ignore"]
            env: prod
            dataset: default
            parallel: 10
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
        workflowsDir.resolve("smoke.yaml").writeText("name: smoke\nfeatures: []")

        val workflow = WorkflowLoader(workflowsDir).load("smoke")

        assertEquals("base", workflow.env)
        assertEquals("default", workflow.dataset)
        assertEquals(1, workflow.parallel)
        assertEquals("standard", workflow.mode)
    }

    @Test
    fun `throws when workflow file is missing`() {
        assertFailsWith<IllegalArgumentException> {
            WorkflowLoader(workflowsDir).load("nonexistent")
        }
    }
}
