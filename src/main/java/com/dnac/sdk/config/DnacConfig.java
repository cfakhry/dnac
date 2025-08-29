// com/dnac/sdk/config/DnacConfig.java
package com.dnac.sdk.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public class DnacConfig {
    private final URI baseUri;               // e.g. https://dnac.example.com
    private final String username;
    private final String password;
    private final boolean insecureTls;       // DEV/LAB ONLY
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public DnacConfig(URI baseUri, String username, String password,
                      boolean insecureTls, Duration connectTimeout, Duration requestTimeout) {
        this.baseUri = Objects.requireNonNull(baseUri);
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
        this.insecureTls = insecureTls;
        this.connectTimeout = connectTimeout == null ? Duration.ofSeconds(15) : connectTimeout;
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;
    }

    public URI baseUri() { return baseUri; }
    public String username() { return username; }
    public String password() { return password; }
    public boolean insecureTls() { return insecureTls; }
    public Duration connectTimeout() { return connectTimeout; }
    public Duration requestTimeout() { return requestTimeout; }
}