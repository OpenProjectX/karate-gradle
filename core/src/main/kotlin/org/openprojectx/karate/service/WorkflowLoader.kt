package org.openprojectx.karate.service

import org.openprojectx.karate.model.Tags
import org.openprojectx.karate.model.Workflow
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Loads a [Workflow] from a YAML file in [workflowsDir].
 *
 * File is resolved as `<workflowsDir>/<name>.yaml`.
 */
class WorkflowLoader(private val workflowsDir: File) {

    fun load(name: String): Workflow {
        val file = workflowsDir.resolve("$name.yaml")
        require(file.exists()) { "Workflow file not found: ${file.absolutePath}" }
        val raw: Map<String, Any> = file.inputStream().use { Yaml().load(it) }
        return mapToWorkflow(raw)
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToWorkflow(raw: Map<String, Any>): Workflow {
        val tagsRaw = raw["tags"] as? Map<String, Any> ?: emptyMap()
        val tags = Tags(
            include = (tagsRaw["include"] as? List<String>) ?: emptyList(),
            exclude = (tagsRaw["exclude"] as? List<String>) ?: emptyList()
        )
        return Workflow(
            name     = raw["name"] as? String ?: error("Workflow YAML must have a 'name' field"),
            features = (raw["features"] as? List<String>) ?: emptyList(),
            tags     = tags,
            env      = raw["env"] as? String ?: "base",
            dataset  = raw["dataset"] as? String ?: "default",
            parallel = (raw["parallel"] as? Int) ?: 1,
            mode     = raw["mode"] as? String ?: "standard"
        )
    }
}
