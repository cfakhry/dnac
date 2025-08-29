// com/dnac/sdk/http/HttpExecutor.java
package com.dnac.sdk.http;

import com.dnac.sdk.auth.TokenProvider;
import com.dnac.sdk.config.DnacConfig;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public final class HttpExecutor {
    private final DnacConfig cfg;
    private final HttpClient http;
    private final TokenProvider tokens;
    private final JsonSupport json;

    public HttpExecutor(DnacConfig cfg, HttpClient http, TokenProvider tokens, JsonSupport json) {
        this.cfg = cfg; this.http = http; this.tokens = tokens; this.json = json;
    }

    public String get(String path, Map<String,String> query) throws Exception {
        String token = tokens.getToken();
        String q = (query == null || query.isEmpty()) ? "" :
                "?" + query.entrySet().stream()
                        .map(e -> urlEnc(e.getKey()) + "=" + urlEnc(e.getValue()))
                        .collect(Collectors.joining("&"));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(cfg.baseUri() + ensureLeadingSlash(path) + q))
                .timeout(cfg.requestTimeout())
                .header("Accept", "application/json")
                .header("X-Auth-Token", token)
                .GET().build();

        return sendWithBasicRetry(req);
    }

    public String postJson(String path, Object body) throws Exception {
        String token = tokens.getToken();
        String payload = json.write(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(cfg.baseUri() + ensureLeadingSlash(path)))
                .timeout(cfg.requestTimeout())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Auth-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return sendWithBasicRetry(req);
    }

    private String sendWithBasicRetry(HttpRequest req) throws Exception {
        int attempts = 0;
        RuntimeException last = null;
        while (attempts++ < 3) {
            try {
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                int sc = res.statusCode();
                if (sc / 100 == 2) return res.body();
                // basic retry on 429/5xx
                if (sc == 429 || sc / 100 == 5) {
                    Thread.sleep(200L * attempts);
                    continue;
                }
                throw new RuntimeException("HTTP " + sc + " - " + res.body());
            } catch (java.net.http.HttpTimeoutException e) {
                last = new RuntimeException("Timeout: " + req.uri(), e);
            }
        }
        throw last != null ? last : new RuntimeException("Failed after retries: " + req.uri());
    }

    private static String ensureLeadingSlash(String p) { return p.startsWith("/") ? p : "/" + p; }
    private static String urlEnc(String s) { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
}