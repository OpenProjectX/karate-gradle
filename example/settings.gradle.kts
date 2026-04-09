pluginManagement {
    // Pull the plugin directly from source — no publishToMavenLocal needed.
    // Gradle resolves org.openprojectx.karate.gradle from the included build.
    includeBuild("..")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "karate-example"
