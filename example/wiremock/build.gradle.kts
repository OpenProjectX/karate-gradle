plugins {
    id("org.openprojectx.karate.gradle")
}

dependencies {
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

regression {
    workflowsDirs.add("src/test/resources/workflows")
    environmentsDirs.add("src/test/resources/environments")
    datasetsRootDir.set("src/test/resources/datasets")

    datasets {
        register("default") {
            path.set("default")
        }
        register("edge-cases") {
            path.set("advanced/edge-cases")
        }
    }
}
