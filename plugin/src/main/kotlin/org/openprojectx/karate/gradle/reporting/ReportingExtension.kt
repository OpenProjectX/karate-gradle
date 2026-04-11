package org.openprojectx.karate.gradle.reporting

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Nested `reporting { }` block inside the `regression { }` extension.
 *
 * ```kotlin
 * regression {
 *     reporting {
 *         allure {
 *             enabled.set(true)
 *         }
 *         reportPortal {
 *             enabled.set(true)
 *             endpoint.set("https://reportportal.example.com")
 *             apiKey.set(providers.environmentVariable("RP_API_KEY"))
 *             project.set("karate-regression")
 *             launch.set("smoke")
 *         }
 *     }
 * }
 * ```
 */
abstract class ReportingExtension @Inject constructor(objects: ObjectFactory) {

    val allure: AllureConfig = objects.newInstance(AllureConfig::class.java)

    val reportPortal: ReportPortalConfig = objects.newInstance(ReportPortalConfig::class.java)

    fun allure(action: Action<AllureConfig>) = action.execute(allure)

    fun reportPortal(action: Action<ReportPortalConfig>) = action.execute(reportPortal)
}
