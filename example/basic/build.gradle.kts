plugins {
    id("org.openprojectx.karate.gradle")
}

regression {
    workflowsDirs.add("src/test/resources/workflows")
    environmentsDirs.add("src/test/resources/environments")
    datasetsRootDir.set("src/test/resources/datasets")

    datasets {
        register("default") {
            path.set("default")
        }
        register("extended") {
            path.set("advanced")
        }
        register("incident-2026-04-09") {
            path.set("incidents/2026-04-09")
        }
    }
}
