package org.openprojectx.karate.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.openprojectx.karate.gradle.reporting.ReportingExtension
import javax.inject.Inject

/**
 * Extension DSL for the `org.openprojectx.karate.gradle` plugin.
 *
 * ```kotlin
 * regression {
 *     workflowsDirs.add("src/test/resources/workflows")
 *     environmentsDirs.add("src/test/resources/environments")
 *     datasetsRootDir.set("src/test/resources/datasets")
 *     datasetProvider.set("local") // local only today
 *
 *     datasets {
 *         register("default") { path.set("datasets/default") }
 *         register("incident") { path.set("datasets/incidents") }
 *     }
 * }
 * ```
 *
 * Multiple directories can be added to [workflowsDirs] and [environmentsDirs].
 * Sources are resolved in order: the first directory containing a matching file wins.
 */
abstract class RegressionExtension @Inject constructor(objects: ObjectFactory) {

    /** Directories searched (in order) for workflow `.conf` / `.json` / `.properties` files. */
    val workflowsDirs: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(listOf("src/test/resources/workflows"))

    /** Directories searched (in order) for environment `.conf` / `.json` / `.properties` files. */
    val environmentsDirs: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(listOf("src/test/resources/environments"))

    /** Dataset provider type. Currently only `"local"` is implemented. */
    val datasetProvider: Property<String> = objects.property(String::class.java)
        .convention("local")

    /** Root directory under which local datasets are stored. */
    val datasetsRootDir: Property<String> = objects.property(String::class.java)
        .convention("datasets")

    val datasets: NamedDomainObjectContainer<DatasetSpec> =
        objects.domainObjectContainer(DatasetSpec::class.java)

    val reporting: ReportingExtension = objects.newInstance(ReportingExtension::class.java)

    fun datasets(action: Action<NamedDomainObjectContainer<DatasetSpec>>) {
        action.execute(datasets)
    }

    fun reporting(action: Action<ReportingExtension>) {
        action.execute(reporting)
    }
}
