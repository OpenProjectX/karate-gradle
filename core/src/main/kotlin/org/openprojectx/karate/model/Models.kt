package org.openprojectx.karate.model

data class Tags(
    val include: List<String> = emptyList(),
    val exclude: List<String> = emptyList()
)

data class Workflow(
    val name: String,
    val features: List<String> = emptyList(),
    val tags: Tags = Tags(),
    val env: String = "base",
    val dataset: String = "default",
    val parallel: Int = 1,
    val mode: String = "standard"
)

data class Dataset(
    val name: String,
    val path: String
)

data class ExecutionContext(
    val workflow: Workflow,
    val datasetPath: String,
    val envConfig: Map<String, Any>
)
