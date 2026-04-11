package org.openprojectx.karate.gradle.runner

import org.junit.jupiter.api.Test
import org.openprojectx.karate.model.Tags
import org.openprojectx.karate.model.Workflow
import kotlin.test.assertEquals

class KarateRunnerAdapterTest {

    @Test
    fun `buildArgs forwards workflow and environment config as system properties`() {
        val workflow = Workflow(
            name = "contract",
            features = listOf("classpath:features/user/get-user.feature"),
            tags = Tags(include = listOf("@contract"), exclude = listOf("@ignore")),
            parallel = 2
        )

        val args = KarateRunnerAdapter.buildArgs(
            workflow = workflow,
            env = "staging",
            envConfig = mapOf(
                "baseUrl" to "https://example.test",
                "connectTimeout" to 2500,
                "tenant" to "sandbox"
            ),
            datasetPath = "/tmp/datasets/default",
            outputDir = "/tmp/reports",
            commitHash = "abc123"
        )

        assertEquals("staging", args.systemProps["karate.env"])
        assertEquals("contract", args.systemProps["karate.workflow"])
        assertEquals("https://example.test", args.systemProps["karate.config.baseUrl"])
        assertEquals("2500", args.systemProps["karate.config.connectTimeout"])
        assertEquals("sandbox", args.systemProps["karate.config.tenant"])
        assertEquals("abc123", args.systemProps["karate.commit"])
    }
}
