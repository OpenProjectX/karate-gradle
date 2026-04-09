package org.openprojectx.karate.provider

import java.io.File
import java.nio.file.Path

/**
 * Resolves datasets from a local directory.
 *
 * Given [datasetsRootDir] and a [datasetPaths] registry (name → relative path),
 * resolves a dataset name to an absolute path under [datasetsRootDir].
 *
 * Falls back to `datasetsRootDir/<datasetName>` when no explicit registration exists.
 */
class LocalDatasetProvider(
    private val datasetsRootDir: File,
    private val datasetPaths: Map<String, String> = emptyMap()
) : DatasetProvider {

    override fun resolve(datasetName: String): Path {
        val relative = datasetPaths[datasetName] ?: datasetName
        val resolved = datasetsRootDir.resolve(relative)
        require(resolved.exists()) {
            "Dataset '$datasetName' resolved to '${resolved.absolutePath}' which does not exist"
        }
        return resolved.toPath()
    }
}
