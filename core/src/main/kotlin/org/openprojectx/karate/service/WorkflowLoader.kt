package org.openprojectx.karate.service

import com.typesafe.config.Config
import org.openprojectx.karate.config.ConfigSource
import org.openprojectx.karate.config.load
import org.openprojectx.karate.model.Tags
import org.openprojectx.karate.model.Workflow
import java.io.File

/**
 * Loads a [Workflow] from one or more [ConfigSource]s.
 *
 * Sources are checked in priority order: first source wins.
 * Supported file formats per source: `.conf` (HOCON), `.json`, `.properties`.
 */
class WorkflowLoader(private val sources: List<ConfigSource>) {

    constructor(workflowsDir: File) : this(listOf(ConfigSource.LocalDirectory(workflowsDir)))

    fun load(name: String): Workflow {
        val config = sources.load(name).resolve()

        require(config.hasPath("name")) {
            "Workflow '$name' not found in configured sources: ${describeSource()}"
        }

        return parseWorkflow(config)
    }

    private fun describeSource(): String =
        sources.filterIsInstance<ConfigSource.LocalDirectory>()
            .joinToString(", ") { it.dir.absolutePath }
            .ifEmpty { sources.toString() }

    private fun parseWorkflow(config: Config): Workflow {
        val tags = if (config.hasPath("tags")) parseTags(config.getConfig("tags")) else Tags()

        return Workflow(
            name     = config.getString("name"),
            features = if (config.hasPath("features")) config.getStringList("features") else emptyList(),
            tags     = tags,
            env      = config.getStringOr("env", "base"),
            dataset  = config.getStringOr("dataset", "default"),
            parallel = if (config.hasPath("parallel")) config.getInt("parallel") else 1,
            mode     = config.getStringOr("mode", "standard"),
        )
    }

    private fun parseTags(config: Config) = Tags(
        include = if (config.hasPath("include")) config.getStringList("include") else emptyList(),
        exclude = if (config.hasPath("exclude")) config.getStringList("exclude") else emptyList(),
    )
}

private fun Config.getStringOr(path: String, default: String): String =
    if (hasPath(path)) getString(path) else default
