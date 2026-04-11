package org.openprojectx.karate.service

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.openprojectx.karate.config.ConfigSource
import org.openprojectx.karate.config.load
import java.io.File

/**
 * Resolves the effective environment configuration by merging `base` with `<env>` config.
 *
 * Supports multiple [ConfigSource]s: sources are checked in priority order (first wins).
 * Supported file formats per source: `.conf` (HOCON), `.json`, `.properties`.
 *
 * Merge order: `<env>` overrides `base` (shallow merge via Typesafe Config fallback).
 *
 * The resolved config is flattened into a `Map<String, Any>` so it can be forwarded as
 * `karate.config.*` system properties. This is intended for flat scalar settings such as
 * URLs, timeouts, tenant names, and simple flags.
 */
class EnvResolver(private val sources: List<ConfigSource>) {

    constructor(environmentsDir: File) : this(listOf(ConfigSource.LocalDirectory(environmentsDir)))

    fun resolve(envName: String): Map<String, Any> {
        val base = sources.load("base")
        val env  = sources.load(envName)

        return env.withFallback(base)
            .resolve()
            .toFlatMap()
    }
}

private fun Config.toFlatMap(): Map<String, Any> =
    entrySet().associate { it.key to it.value.unwrapped() }
