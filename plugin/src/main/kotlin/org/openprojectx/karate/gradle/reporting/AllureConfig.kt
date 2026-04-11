package org.openprojectx.karate.gradle.reporting

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration for the [Allure](https://allurereport.org) reporting integration.
 *
 * When enabled, `allure-junit5` is added to `testImplementation` and
 * `-Dallure.results.directory` is forwarded to the forked Karate JVM.
 *
 * ```kotlin
 * reporting {
 *     allure {
 *         enabled.set(true)
 *         resultsDir.set("allure-results")   // relative to build dir, default
 *     }
 * }
 * ```
 *
 * Generate the HTML report after the run:
 * ```bash
 * allure serve build/allure-results
 * ```
 */
abstract class AllureConfig @Inject constructor(objects: ObjectFactory) {

    val enabled: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /** Allure JUnit5 adapter version (`io.qameta.allure:allure-junit5`). */
    val version: Property<String> = objects.property(String::class.java)
        .convention("2.27.0")

    /**
     * Directory where Allure writes raw result JSON files.
     * Relative to the project's build directory. Default: `allure-results`.
     */
    val resultsDir: Property<String> = objects.property(String::class.java)
        .convention("allure-results")
}
