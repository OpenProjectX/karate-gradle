package example.wiremock.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public final class WireMockSupport {

    private static WireMockServer server;

    private WireMockSupport() {
    }

    public static synchronized String ensureStarted() {
        if (server == null) {
            server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (server != null && server.isRunning()) {
                    server.stop();
                }
            }));
        }
        return server.baseUrl();
    }
}
