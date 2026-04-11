plugins {
    base
}

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks.register("regressionRunAll") {
    group = "regression"
    description = "Run regressionRun for all sample subprojects"
    dependsOn(
        subprojects.map { it.tasks.named("regressionRun") }
    )
}
