package org.openprojectx.karate.gradle.reporting

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration for the [ReportPortal](https://reportportal.io) reporting integration.
 *
 * When enabled, `agent-java-junit5` is added to `testImplementation` and all `rp.*`
 * system properties are forwarded to the forked Karate JVM so the agent can stream
 * results to the ReportPortal server in real time.
 *
 * ```kotlin
 * reporting {
 *     reportPortal {
 *         enabled.set(true)
 *         endpoint.set("https://reportportal.example.com")
 *         apiKey.set(providers.environmentVariable("RP_API_KEY"))
 *         project.set("karate-regression")
 *         launch.set("smoke")
 *         attributes.set(listOf("team:backend", "env:staging"))
 *     }
 * }
 * ```
 *
 * Keep [apiKey] out of version control — supply it via an environment variable or
 * Gradle property (`-Prp.apiKey=...`).
 */
abstract class ReportPortalConfig @Inject constructor(objects: ObjectFactory) {

    val enabled: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /** ReportPortal JUnit5 agent version (`com.epam.reportportal:agent-java-junit5`). */
    val agentVersion: Property<String> = objects.property(String::class.java)
        .convention("5.4.1")

    /** ReportPortal server URL, e.g. `https://reportportal.example.com`. */
    val endpoint: Property<String> = objects.property(String::class.java)

    /** API key (token) for authenticating with the ReportPortal server. */
    val apiKey: Property<String> = objects.property(String::class.java)

    /** ReportPortal project name. */
    val project: Property<String> = objects.property(String::class.java)

    /** Launch name displayed in ReportPortal. */
    val launch: Property<String> = objects.property(String::class.java)

    /** Optional human-readable launch description. */
    val description: Property<String> = objects.property(String::class.java)

    /**
     * Launch attributes in `key:value` or plain `value` format.
     * Multiple attributes are joined with `;` as required by the agent.
     */
    val attributes: ListProperty<String> = objects.listProperty(String::class.java)
}
