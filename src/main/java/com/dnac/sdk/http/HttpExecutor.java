// com/dnac/sdk/http/HttpExecutor.java
package com.dnac.sdk.http;

import com.dnac.sdk.auth.TokenProvider;
import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.exceptions.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
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
        String q = (query == null || query.isEmpty()) ? "" :
                "?" + query.entrySet().stream()
                        .map(e -> urlEnc(e.getKey()) + "=" + urlEnc(e.getValue()))
                        .collect(Collectors.joining("&"));
        URI uri = URI.create(cfg.baseUri() + ensureLeadingSlash(path) + q);
        return sendWithPolicy("GET", uri, null, "application/json");
    }

    public String postJson(String path, Object body) throws Exception {
        String payload = json.write(body);
        URI uri = URI.create(cfg.baseUri() + ensureLeadingSlash(path));
        return sendWithPolicy("POST", uri, payload, "application/json");
    }

    // --- Core send policy with retries, 401 refresh, Retry-After, jitter ---
    private String sendWithPolicy(String method, URI uri, String payload, String contentType) throws Exception {
        int maxAttempts = 5;
        int attempt = 0;
        boolean refreshedOn401 = false;

        while (true) {
            attempt++;

            String token = tokens.getToken();
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(cfg.requestTimeout())
                    .header("Accept", "application/json")
                    .header("X-Auth-Token", token);

            if (payload != null) rb.header("Content-Type", contentType);

            HttpRequest req = switch (method) {
                case "GET"  -> rb.GET().build();
                case "POST" -> rb.POST(HttpRequest.BodyPublishers.ofString(payload)).build();
                default     -> throw new IllegalArgumentException("Unsupported method: " + method);
            };

            HttpResponse<String> res;
            try {
                res = http.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException e) {
                if (attempt >= maxAttempts) throw new DnacTimeoutException(uri, "Timeout", e);
                sleepBackoff(attempt, /*retryAfterMs*/ Optional.empty());
                continue;
            } catch (Exception e) {
                if (attempt >= maxAttempts) throw new DnacTransportException(uri, "Transport error", e);
                sleepBackoff(attempt, Optional.empty());
                continue;
            }

            int sc = res.statusCode();
            String body = res.body();
            String requestId = firstHeader(res, "X-Request-Id").orElse(firstHeader(res, "x-request-id").orElse(null));

            // Success
            if (sc / 100 == 2) return body;

            // 401: try one forced refresh once
            if (sc == 401 && !refreshedOn401) {
                // force refresh by invalidating cached token (simple approach: delegate provides fresh on next call)
                refreshedOn401 = true;
                // Some TokenProviders may need an explicit "force refresh" method; if so, extend TokenProvider.
                sleepBackoff(attempt, Optional.empty());
                continue;
            }

            // 429 / 5xx: retry with backoff (+ respect Retry-After)
            if (sc == 429 || sc / 100 == 5) {
                if (attempt >= maxAttempts) throw toException(uri, sc, body, requestId);
                Optional<Long> retryAfterMs = parseRetryAfterMillis(res);
                sleepBackoff(attempt, retryAfterMs);
                continue;
            }

            // Other client errors: map and throw
            throw toException(uri, sc, body, requestId);
        }
    }

    private static Optional<String> firstHeader(HttpResponse<?> res, String name) {
        return res.headers().firstValue(name);
    }

    private static Optional<Long> parseRetryAfterMillis(HttpResponse<?> res) {
        Optional<String> ra = firstHeader(res, "Retry-After");
        if (ra.isEmpty()) return Optional.empty();
        String v = ra.get().trim();
        try {
            // common numeric seconds
            long seconds = Long.parseLong(v);
            return Optional.of(Duration.of(seconds, ChronoUnit.SECONDS).toMillis());
        } catch (NumberFormatException ignore) {
            // RFC allows HTTP-date; parsing omitted for brevity
            return Optional.empty();
        }
    }

    private static void sleepBackoff(int attempt, Optional<Long> retryAfterMs) {
        long base = (long) Math.min(200L * (1L << Math.min(attempt, 6)), 5000L); // capped backoff
        long jitter = ThreadLocalRandom.current().nextLong(0, 250);
        long delay = base + jitter;
        if (retryAfterMs.isPresent()) delay = Math.max(delay, retryAfterMs.get());
        try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
    }

    private static String ensureLeadingSlash(String p) { return p.startsWith("/") ? p : "/" + p; }
    private static String urlEnc(String s) { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }

    // --- Error mapping ---
    private DnacException toException(URI uri, int status, String body, String reqId) {
        ApiErrorPayload payload = ApiErrorPayload.tryParse(body, json);
        return switch (status / 100) {
            case 4 -> new DnacClientErrorException(uri, status, payload, reqId);
            case 5 -> new DnacServerErrorException(uri, status, payload, reqId);
            default -> new DnacHttpStatusException(uri, status, payload, reqId);
        };
    }
}
