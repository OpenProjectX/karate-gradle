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

// java-gradle-plugin auto-generates two publications alongside the root build's "mavenJava":
//   - "pluginMaven"  (the plugin jar artifact, artifactId = project.name = "plugin")
//   - "<id>PluginMarkerMaven"  (the plugin marker POM that resolves plugin ID → coordinates)
//
// Maven Central rejects any artifact whose POM is missing name / description / url /
// license / scm / developers.  Patch those fields here with afterEvaluate so that
// java-gradle-plugin has already registered all its publications before we reach them.
afterEvaluate {
    // "pluginMaven" and "mavenJava" share the same Maven coordinates (groupId:plugin:version).
    // publications.removeIf() doesn't remove already-registered publish tasks (Gradle eagerly
    // creates them when publications are added). Disable the mavenJava publish tasks instead
    // so only pluginMaven — the canonical Gradle plugin publication — is uploaded.
    tasks.withType<AbstractPublishToMaven>().matching { it.name.contains("MavenJava") }
        .configureEach { enabled = false }

    // Patch POM metadata onto the two auto-generated publications.
    // Maven Central requires name / description / url / license / scm / developers
    // on every artifact; java-gradle-plugin does not set these automatically.
    extensions.configure<PublishingExtension>("publishing") {
        publications.withType<MavenPublication>()
            .matching { it.name != "mavenJava" }
            .configureEach {
                pom {
                    name.set("karate-gradle")
                    description.set("Gradle plugin for workflow-driven Karate regression testing")
                    url.set("https://github.com/OpenProjectX/karate-gradle")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("OpenProjectX")
                            name.set("OpenProjectX")
                            email.set("admin@openprojectx.org")
                        }
                    }

                    scm {
                        url.set("https://github.com/OpenProjectX/karate-gradle")
                        connection.set("scm:git:https://github.com/OpenProjectX/karate-gradle.git")
                        developerConnection.set("scm:git:ssh://git@github.com:OpenProjectX/karate-gradle.git")
                    }
                }
            }
    }
}
