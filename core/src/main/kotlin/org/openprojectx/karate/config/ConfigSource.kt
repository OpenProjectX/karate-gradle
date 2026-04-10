package org.openprojectx.karate.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

/**
 * A named source of configuration that can be resolved by a logical name
 * (e.g., a workflow name or environment name).
 *
 * Sources are composed with priority: the first source in a list wins over later ones.
 */
sealed class ConfigSource {

    abstract fun forName(name: String): Config

    /**
     * Loads from a local directory. Tries `.conf`, `.json`, `.properties` in that order.
     * Returns an empty [Config] when no matching file is found.
     */
    class LocalDirectory(val dir: File) : ConfigSource() {

        override fun forName(name: String): Config =
            EXTENSIONS
                .map { ext -> dir.resolve("$name$ext") }
                .firstOrNull { it.exists() }
                ?.let { ConfigFactory.parseFile(it) }
                ?: ConfigFactory.empty()

        companion object {
            private val EXTENSIONS = listOf(".conf", ".json", ".properties")
        }
    }

    /**
     * Loads from the classpath. Tries all formats supported by [ConfigFactory.parseResourcesAnySyntax].
     * Returns an empty [Config] when the resource is not present.
     */
    class ClasspathResource(val basePath: String = "") : ConfigSource() {

        override fun forName(name: String): Config {
            val resource = if (basePath.isEmpty()) name else "$basePath/$name"
            return ConfigFactory.parseResourcesAnySyntax(resource)
        }
    }

    /**
     * Exposes JVM system properties as a [Config].
     * Useful as a highest-priority override source.
     */
    object SystemProperties : ConfigSource() {

        override fun forName(name: String): Config = ConfigFactory.systemProperties()
    }
}

/**
 * Merges configs from all [sources], where the first source has the highest priority.
 * Returns [ConfigFactory.empty] when the list is empty.
 */
fun List<ConfigSource>.load(name: String): Config =
    map { it.forName(name) }
        .reduceOrNull { high, low -> high.withFallback(low) }
        ?: ConfigFactory.empty()
