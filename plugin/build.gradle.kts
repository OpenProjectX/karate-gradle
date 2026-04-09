plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":core"))
    // karate-core needed at compile time for KarateRunnerAdapter (main class constant only)
    compileOnly(libs.karateCore)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("karateRegression") {
            id = "org.openprojectx.karate.gradle"
            implementationClass = "org.openprojectx.karate.gradle.KarateRegressionPlugin"
        }
    }
}
