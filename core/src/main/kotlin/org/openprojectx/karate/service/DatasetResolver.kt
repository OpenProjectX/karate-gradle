package org.openprojectx.karate.service

import org.openprojectx.karate.provider.DatasetProvider
import java.nio.file.Path

/**
 * Thin orchestration layer that delegates dataset resolution to a [DatasetProvider].
 */
class DatasetResolver(private val provider: DatasetProvider) {

    fun resolve(datasetName: String): Path = provider.resolve(datasetName)
}
