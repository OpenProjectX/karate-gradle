package org.openprojectx.karate.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.yaml.snakeyaml.Yaml
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
     * Loads from a local directory. Tries extensions in priority order:
     * `.conf` (HOCON) → `.json` → `.yaml` → `.yml` → `.properties`.
     * Returns an empty [Config] when no matching file is found.
     */
    class LocalDirectory(val dir: File) : ConfigSource() {

        override fun forName(name: String): Config {
            TYPESAFE_EXTENSIONS.forEach { ext ->
                val file = dir.resolve("$name$ext")
                if (file.exists()) return ConfigFactory.parseFile(file)
            }
            YAML_EXTENSIONS.forEach { ext ->
                val file = dir.resolve("$name$ext")
                if (file.exists()) return parseYaml(file)
            }
            return ConfigFactory.empty()
        }

        companion object {
            private val TYPESAFE_EXTENSIONS = listOf(".conf", ".json", ".properties")
            private val YAML_EXTENSIONS     = listOf(".yaml", ".yml")

            @Suppress("UNCHECKED_CAST")
            private fun parseYaml(file: File): Config {
                val raw = file.inputStream().use { Yaml().load<Any>(it) }
                return if (raw is Map<*, *>) ConfigFactory.parseMap(raw as Map<String, Any>)
                       else ConfigFactory.empty()
            }
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
