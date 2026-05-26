package org.openprojectx.karate.gradle.runner

/**
 * Prefix for user-owned properties that should be passed unchanged to Karate.
 *
 * Example:
 * `-Dkarate.user.token=abc` is available as `karate.properties['karate.user.token']`.
 */
object KaratePassThroughProperties {
    const val PREFIX = "karate.user."
}
