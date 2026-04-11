package org.openprojectx.karate.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvResolverTest {

    @TempDir
    lateinit var envDir: File

    @Test
    fun `merges base and env config with env taking precedence`() {
        envDir.resolve("base.conf").writeText(
            """
            timeout = 5000
            baseUrl = "http://localhost"
            """.trimIndent()
        )
        envDir.resolve("prod.conf").writeText(
            """
            baseUrl = "https://api.prod.com"
            """.trimIndent()
        )

        val config = EnvResolver(envDir).resolve("prod")

        assertEquals("https://api.prod.com", config["baseUrl"])
        assertEquals(5000, config["timeout"])
    }

    @Test
    fun `returns base only when env file is absent`() {
        envDir.resolve("base.conf").writeText("timeout = 3000")

        val config = EnvResolver(envDir).resolve("nonexistent")

        assertEquals(3000, config["timeout"])
    }

    @Test
    fun `returns empty map when no files exist`() {
        val config = EnvResolver(envDir).resolve("missing")

        assertTrue(config.isEmpty())
    }

    @Test
    fun `merges env config across multiple source directories`() {
        val sharedDir = envDir.resolve("shared").also { it.mkdirs() }
        val projectDir = envDir.resolve("project").also { it.mkdirs() }

        sharedDir.resolve("base.conf").writeText(
            """
            timeout = 5000
            tenant = shared-default
            """.trimIndent()
        )
        projectDir.resolve("staging.conf").writeText(
            """
            baseUrl = "https://api.staging.com"
            tenant = project-staging
            """.trimIndent()
        )

        val resolver = EnvResolver(
            listOf(
                org.openprojectx.karate.config.ConfigSource.LocalDirectory(projectDir),
                org.openprojectx.karate.config.ConfigSource.LocalDirectory(sharedDir),
            )
        )
        val config = resolver.resolve("staging")

        assertEquals("https://api.staging.com", config["baseUrl"])     // projectDir
        assertEquals("project-staging", config["tenant"])              // projectDir wins
        assertEquals(5000, config["timeout"])                          // sharedDir fallback
    }
}
