package org.openprojectx.karate.provider

import java.nio.file.Path

/**
 * Strategy for resolving a named dataset to a filesystem path.
 *
 * Built-in: [LocalDatasetProvider].
 * Future: S3DatasetProvider, custom implementations.
 */
interface DatasetProvider {
    fun resolve(datasetName: String): Path
}
