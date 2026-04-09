package org.openprojectx.karate.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension DSL for the `org.openprojectx.karate.gradle` plugin.
 *
 * ```kotlin
 * regression {
 *     workflowsDir.set("src/test/resources/workflows")
 *     featuresDir.set("src/test/resources/features")
 *     testDataDir.set("src/test/resources/test-data")
 *     datasetProvider.set("local")
 *
 *     datasets {
 *         register("default") { path.set("datasets/default") }
 *         register("incident") { path.set("datasets/incidents") }
 *     }
 * }
 * ```
 */
abstract class RegressionExtension @Inject constructor(objects: ObjectFactory) {

    val workflowsDir: Property<String> = objects.property(String::class.java)
        .convention("src/test/resources/workflows")

    val featuresDir: Property<String> = objects.property(String::class.java)
        .convention("src/test/resources/features")

    val testDataDir: Property<String> = objects.property(String::class.java)
        .convention("src/test/resources/test-data")

    val environmentsDir: Property<String> = objects.property(String::class.java)
        .convention("src/test/resources/environments")

    /** "local" | "s3" | custom fully-qualified class name */
    val datasetProvider: Property<String> = objects.property(String::class.java)
        .convention("local")

    /** Root directory under which local datasets are stored. */
    val datasetsRootDir: Property<String> = objects.property(String::class.java)
        .convention("datasets")

    val datasets: NamedDomainObjectContainer<DatasetSpec> =
        objects.domainObjectContainer(DatasetSpec::class.java)

    fun datasets(action: Action<NamedDomainObjectContainer<DatasetSpec>>) {
        action.execute(datasets)
    }
}
