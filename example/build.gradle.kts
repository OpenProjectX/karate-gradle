plugins {
    id("org.openprojectx.karate.gradle")
}

regression {
    workflowsDir.set("src/test/resources/workflows")
    featuresDir.set("src/test/resources/features")
    environmentsDir.set("src/test/resources/environments")
    datasetsRootDir.set("src/test/resources/datasets")

    datasets {
        register("default") {
            path.set("default")
        }
    }
}
