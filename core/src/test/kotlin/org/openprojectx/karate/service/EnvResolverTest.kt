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
    fun `merges base and env yaml with env taking precedence`() {
        envDir.resolve("base.yaml").writeText("timeout: 5000\nbaseUrl: http://localhost")
        envDir.resolve("prod.yaml").writeText("baseUrl: https://api.prod.com")

        val config = EnvResolver(envDir).resolve("prod")

        assertEquals("https://api.prod.com", config["baseUrl"])
        assertEquals(5000, config["timeout"])
    }

    @Test
    fun `returns base only when env file is absent`() {
        envDir.resolve("base.yaml").writeText("timeout: 3000")

        val config = EnvResolver(envDir).resolve("nonexistent")

        assertEquals(3000, config["timeout"])
    }

    @Test
    fun `returns empty map when no files exist`() {
        val config = EnvResolver(envDir).resolve("missing")
        assertTrue(config.isEmpty())
    }
}
