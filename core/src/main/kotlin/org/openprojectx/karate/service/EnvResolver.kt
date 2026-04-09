package org.openprojectx.karate.service

import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Resolves the effective environment configuration by merging `base.yaml` with `<env>.yaml`.
 *
 * Merge order: `base.yaml` → `<env>.yaml` (env values override base, shallow merge).
 */
class EnvResolver(private val environmentsDir: File) {

    fun resolve(envName: String): Map<String, Any> {
        val base = loadIfExists("base")
        val env  = loadIfExists(envName)
        return base + env
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadIfExists(name: String): Map<String, Any> {
        val file = environmentsDir.resolve("$name.yaml")
        if (!file.exists()) return emptyMap()
        return file.inputStream().use { Yaml().load(it) as? Map<String, Any> ?: emptyMap() }
    }
}
