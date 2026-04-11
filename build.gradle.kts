import net.researchgate.release.ReleaseExtension
import org.asciidoctor.gradle.jvm.AsciidoctorTask

val ciGradleUserHome: String = System.getenv("GRADLE_USER_HOME")
    ?: layout.buildDirectory.dir("ci-gradle-home").get().asFile.absolutePath

plugins {
    `maven-publish`
    signing
    id("org.asciidoctor.jvm.convert") version "4.0.2"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0" // nexus publish/close/release
    id("net.researchgate.release") version "3.1.0"

}

tasks.named<AsciidoctorTask>("asciidoctor") {
    group = "documentation"
    description = "Generates HTML documentation from AsciiDoc sources"
    notCompatibleWithConfigurationCache("Asciidoctor task configuration is not configuration-cache compatible in this build")
    setSourceDir(file("doc"))
    sources {
        include("user-guide.adoc")
    }
    setOutputDir(layout.buildDirectory.dir("docs").get().asFile)
    doLast {
        copy {
            from(layout.buildDirectory.file("docs/user-guide.html"))
            into(layout.buildDirectory.dir("docs"))
            rename { "index.html" }
        }
    }
}

val syncDocsVersion by tasks.registering {
    val versionedDocFiles = layout.files(
        layout.projectDirectory.file("README.md"),
        layout.projectDirectory.file("doc/user-guide.adoc"),
    )

    group = "documentation"
    description = "Syncs plugin version snippets in README and user guide to project.version"

    inputs.property("pluginVersion", project.version.toString())
    outputs.files(versionedDocFiles)
    inputs.files(versionedDocFiles)

    doLast {
        val pluginVersion = inputs.properties["pluginVersion"].toString()
        val pluginVersionSnippetRegex =
            Regex("""(id\("org\.openprojectx\.karate\.gradle"\) version ")([^"]+)(")""")

        inputs.files.files.forEach { file ->
            val original = file.readText()
            val updated = pluginVersionSnippetRegex.replace(original) { match ->
                "${match.groupValues[1]}$pluginVersion${match.groupValues[3]}"
            }

            if (original != updated) {
                file.writeText(updated)
            }
        }
    }
}

val verifyBasicExampleForRelease by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs the basic standalone example as a release gate"
    workingDir = rootDir
//    environment("GRADLE_USER_HOME", "/data/.gradle")
    environment("JAVA_HOME", System.getProperty("java.home"))
    commandLine(
        "./gradlew",
        "-p",
        "example",
        ":basic:regressionRun",
        "-Pworkflow=smoke",
        "-Penv=staging"
    )
}

val verifyWiremockExampleForRelease by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs the WireMock standalone example as a release gate"
    workingDir = rootDir
//    environment("GRADLE_USER_HOME", ciGradleUserHome)
    environment("JAVA_HOME", System.getProperty("java.home"))
    commandLine(
        "./gradlew",
        "-p",
        "example",
        ":wiremock:regressionRun",
        "-Pworkflow=regression"
    )
}

val verifyReleaseExamples by tasks.registering {
    group = "verification"
    description = "Runs plugin and example verification required before release"
    dependsOn(
        ":plugin:test",
        verifyBasicExampleForRelease,
        verifyWiremockExampleForRelease
    )
}

allprojects {
    group = "org.openprojectx.karate.gradle"
}


subprojects {
    tasks.register<DependencyReportTask>("allDependencies") {}

    // Apply to every module (safe even if a module doesn't publish)
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    // Configure publishing only when the project has a Java component (Kotlin/JVM typically applies java too)
    plugins.withId("java") {

        // ✅ Ensure required artifacts exist for Maven Central
        extensions.configure<JavaPluginExtension>("java") {
            withSourcesJar()
            withJavadocJar()
        }

        // Kotlin-only modules can produce "empty-ish" Javadoc; don't fail the build on doclint/errors
        tasks.withType(Javadoc::class.java).configureEach {
            isFailOnError = false
        }


        extensions.configure<PublishingExtension>("publishing") {
            publications {
                // Create once per project
                if (findByName("mavenJava") == null) {
                    create<MavenPublication>("mavenJava") {
                        from(components["java"])

                        // Prefer explicit artifactId; by default it's project.name
                        artifactId = project.name

                        pom {
                            // Module-specific name/description; override per-module if you want
                            name.set(project.name)
                            description.set("KARATE-GRADLE Spring Boot starter")
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
        }
    }

    // Signing: only configure keys if provided (keeps local dev painless)
    extensions.configure<SigningExtension>("signing") {
        val keyFile = System.getenv("SIGNING_KEY_FILE")
        val keyPass = System.getenv("SIGNING_KEY_PASSWORD")

        if (!keyFile.isNullOrBlank()) {
            val keyText = file(keyFile).readText()
            useInMemoryPgpKeys(keyText, keyPass)

            // Sign all publications created in this subproject
            val publishing = extensions.findByType(PublishingExtension::class.java)
            if (publishing != null) {
                sign(publishing.publications)
            }
        }
    }

    // Fix implicit dependency between sign and publish tasks (Gradle 7+).
    // java-gradle-plugin generates additional publications (pluginMaven + marker).
    // Their sign tasks produce .asc files that publish tasks consume, but Gradle
    // does not wire the ordering automatically across different publication names.
    afterEvaluate {
        tasks.withType<PublishToMavenRepository>().configureEach {
            dependsOn(tasks.withType<Sign>())
        }
    }
}


nexusPublishing {

    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            logger.info("using username: ${System.getenv("OSSRH_USERNAME")}")

            password.set(System.getenv("OSSRH_PASSWORD"))
            logger.info("using password: ${System.getenv("OSSRH_PASSWORD")}")

        }
    }
}

configure<ReleaseExtension> {
    buildTasks.set(
        listOf(
            "syncDocsVersion",
            "verifyReleaseExamples",
            "publishToSonatype",
            "closeAndReleaseSonatypeStagingRepository"
        )
    )
    versionPropertyFile.set("gradle.properties")
    tagTemplate.set("\$name-\$version")

    with(git) {
        requireBranch.set("master")
    }
}
