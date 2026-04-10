package runner;

import com.intuit.karate.junit5.Karate;

/**
 * JUnit5 runner for Karate features.
 *
 * This class is the bridge between JUnit5 and Karate. Running {@code ./gradlew test}
 * executes the methods below via the JUnit5 platform, which fires standard JUnit5
 * lifecycle events. Reporting agents — Allure ({@code allure-junit5}) and ReportPortal
 * ({@code agent-java-junit5}) — hook into those events automatically.
 *
 * <p>Usage:
 * <pre>
 *   # Run tests and generate Allure report
 *   ./gradlew :basic:test allureReport
 *
 *   # Open the report in the browser
 *   ./gradlew :basic:allureServe
 * </pre>
 *
 * <p>Note: {@code regressionRun} uses {@code com.intuit.karate.Main} directly and does
 * not fire JUnit5 events, so Allure/ReportPortal results come from <em>this</em> runner,
 * not from {@code regressionRun}.
 */
class AllureRunner {

    @Karate.Test
    Karate smoke() {
        return Karate.run("classpath:features")
                .tags("@smoke")
                .relativeTo(getClass());
    }

    @Karate.Test
    Karate regression() {
        return Karate.run("classpath:features")
                .tags("@regression")
                .relativeTo(getClass());
    }
}
