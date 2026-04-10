package runner;

import com.intuit.karate.junit5.Karate;

/**
 * JUnit5 runner for Karate features in the wiremock module.
 *
 * This class is the bridge between JUnit5 and Karate. Running {@code ./gradlew test}
 * executes the methods below via the JUnit5 platform, which fires standard JUnit5
 * lifecycle events. The ReportPortal agent ({@code agent-java-junit5}) hooks into
 * those events and streams results to a running ReportPortal server in real time.
 *
 * <p>WireMock is started automatically by {@code karate-config.js} via
 * {@code WireMockSupport.ensureStarted()} — no extra lifecycle setup is needed here.
 *
 * <p>Usage:
 * <pre>
 *   # Enable ReportPortal in build.gradle.kts:
 *   #   reporting { reportPortal { enabled.set(true) } }
 *
 *   # Export credentials:
 *   export RP_ENDPOINT=https://reportportal.example.com
 *   export RP_API_KEY=your-api-key
 *
 *   # Run tests (streams results to ReportPortal in real time):
 *   ./gradlew :wiremock:test
 * </pre>
 *
 * <p>Note: {@code regressionRun} uses {@code com.intuit.karate.Main} directly and does
 * not fire JUnit5 events, so ReportPortal results come from <em>this</em> runner,
 * not from {@code regressionRun}.
 */
class ReportPortalRunner {

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
