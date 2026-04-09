package org.openprojectx.karate.gradle

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * One dataset registration inside the `datasets { register("name") { path.set(...) } }` DSL block.
 */
abstract class DatasetSpec @Inject constructor(
    private val _name: String,
    objects: ObjectFactory
) : Named {

    override fun getName(): String = _name

    /** Relative path under `datasetsRootDir`. Defaults to the dataset name itself. */
    val path: Property<String> = objects.property(String::class.java).convention(_name)
}
