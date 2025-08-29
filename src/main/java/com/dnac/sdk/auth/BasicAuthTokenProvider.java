// com/dnac/sdk/auth/BasicAuthTokenProvider.java
package com.dnac.sdk.auth;

import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.http.JsonSupport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.net.http.*;

public final class BasicAuthTokenProvider implements TokenProvider {
    private final DnacConfig cfg;
    private final HttpClient http;
    private final JsonSupport json;

    public BasicAuthTokenProvider(DnacConfig cfg, HttpClient http, JsonSupport json) {
        this.cfg = cfg; this.http = http; this.json = json;
    }

    @Override public String getToken() throws Exception {
        String auth = cfg.username() + ":" + cfg.password();
        String basic = "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(cfg.baseUri() + "/dna/system/api/v1/auth/token"))
                .timeout(cfg.requestTimeout())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", basic)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("Token request failed: " + res.statusCode() + " - " + res.body());
        }
        TokenResponse tr = json.read(res.body(), TokenResponse.class);
        if (tr.Token == null || tr.Token.isBlank()) {
            throw new IllegalStateException("Token missing in response");
        }
        return tr.Token;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class TokenResponse {
        @JsonProperty("Token")
        public String Token;
    }
}