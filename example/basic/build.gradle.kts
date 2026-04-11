plugins {
    id("org.openprojectx.karate.gradle")
    id("io.qameta.allure") version "3.0.1"
}

// ── Allure HTML report generation ────────────────────────────────────────────
// The io.qameta.allure plugin adds two tasks:
//   allureReport  – converts build/allure-results/ → build/reports/allure-report/index.html
//   allureServe   – generates and opens the report in the browser
//
// allure-junit5 is already added to testImplementation by regression { reporting { allure } }.
// Disable autoconfigure so the plugin does not add it a second time.
allure {
    adapter {
        autoconfigure.set(false)
    }
}

regression {
    workflowsDirs.add("src/test/resources/workflows")
    environmentsDirs.add("src/test/resources/environments")
    datasetsRootDir.set("src/test/resources/datasets")

    // ── Reporting ─────────────────────────────────────────────────────────────
    // Allure: adds allure-junit5 to testImplementation and wires
    //         -Dallure.results.directory on the standard `test` task.
    //
    //   Run:  ./gradlew :basic:test allureReport
    //   Open: ./gradlew :basic:allureServe
    //
    // Note: regressionRun uses com.intuit.karate.Main (not JUnit5), so Allure
    //       results are only produced by `./gradlew test`, not `regressionRun`.
    reporting {
        allure {
            enabled.set(true)
        }
    }

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

// dataset.path is needed by Karate features when run via `./gradlew test`
// (regressionRun injects it from the workflow config; the test task does not).
tasks.withType<Test>().configureEach {
    systemProperty("dataset.path",
        "${project.projectDir}/src/test/resources/datasets/default")
}
