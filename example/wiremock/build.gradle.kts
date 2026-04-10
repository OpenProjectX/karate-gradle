plugins {
    id("org.openprojectx.karate.gradle")
}

dependencies {
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ── JUnit5 test task ─────────────────────────────────────────────────────────
// System properties needed when running features directly via ReportPortalRunner
// (i.e. not via regressionRun, which injects these from workflow + env config).
// WireMock itself is started automatically by karate-config.js via WireMockSupport.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("dataset.path",
        "${project.projectDir}/src/test/resources/datasets/default")
}

regression {
    workflowsDirs.add("src/test/resources/workflows")
    environmentsDirs.add("src/test/resources/environments")
    datasetsRootDir.set("src/test/resources/datasets")

    // ── Reporting ─────────────────────────────────────────────────────────────
    // ReportPortal: streams JUnit5 test results to a ReportPortal server in
    // real time. Requires a running RP instance.
    //
    //   Local server via Docker:
    //     https://github.com/reportportal/reportportal#deployment
    //
    //   Enable:
    //     1. Set enabled.set(true)
    //     2. Export RP_ENDPOINT and RP_API_KEY environment variables
    //     3. ./gradlew :wiremock:test
    //
    // Note: regressionRun uses com.intuit.karate.Main (not JUnit5), so results
    //       are only streamed by `./gradlew test`, not `regressionRun`.
    reporting {
        reportPortal {
            enabled.set(false)
            endpoint.set(providers.environmentVariable("RP_ENDPOINT").orElse("http://localhost:8080"))
            apiKey.set(providers.environmentVariable("RP_API_KEY"))
            project.set("karate-wiremock")
            launch.set("payment-lifecycle")
            description.set("Karate payment lifecycle tests against WireMock")
            attributes.set(listOf("service:payments", "transport:wiremock"))
        }
    }

    datasets {
        register("default") {
            path.set("default")
        }
        register("edge-cases") {
            path.set("advanced/edge-cases")
        }
    }
}
