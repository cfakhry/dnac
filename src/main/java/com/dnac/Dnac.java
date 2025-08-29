package com.dnac;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public class Dnac {
    private static Properties props;
    private static HttpClient http;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String baseUrl;
    private static String username;
    private static String password;

    // simple token cache with coarse expiry window
    private static String cachedToken;
    private static Instant cachedTokenExp = Instant.EPOCH;

    public static void initialize(Properties properties) throws Exception {
        if (properties == null) throw new Exception("Properties cannot be null");

        props = properties;
        baseUrl  = trimTrailingSlash(required(props, "dnac.host"));
        username = required(props, "dnac.username");
        password = required(props, "dnac.password");

        // ---- Optional insecure SSL (DEV/LAB ONLY) ----
        boolean insecure = Boolean.parseBoolean(props.getProperty("dnac.insecure", "false"));

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15));

        if (insecure) {
            TrustManager[] trustAll = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] c, String a) {}
                        public void checkServerTrusted(X509Certificate[] c, String a) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());
            builder.sslContext(sc);

            // (Optional, risky) disable host name verification for JDK HttpClient
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        }
        // ----------------------------------------------

        http = builder.build();
    }

    public static String username() { return username; }
    public static String password() { return password; }

    public static String getToken() throws Exception {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExp)) {
            return cachedToken;
        }
        // POST /dna/system/api/v1/auth/token with Basic auth
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/dna/system/api/v1/auth/token"))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> res = send(req);

        if (res.statusCode() / 100 != 2) {
            throw new Exception("Token request failed: HTTP " + res.statusCode() + " - " + res.body());
        }

        TokenResponse tokenRes = mapper.readValue(res.body(), TokenResponse.class);
        if (tokenRes.Token == null || tokenRes.Token.isBlank()) {
            throw new Exception("Token missing in response");
        }
        cachedToken = tokenRes.Token;
        // DNAC tokens are ~1 hour; cache for 55 minutes
        cachedTokenExp = Instant.now().plus(Duration.ofMinutes(55));
        return cachedToken;
    }

    public static List<Device> getDevices() throws Exception {
        String json = getRaw("/dna/intent/api/v1/network-device");
        NetworkDeviceResponse resp = mapper.readValue(json, NetworkDeviceResponse.class);
        return resp.response == null ? List.of() : resp.response;
    }

    public static String getRaw(String absoluteOrRootedPath) throws Exception {
        String token = getToken();
        String url = absoluteOrRootedPath.startsWith("http")
                ? absoluteOrRootedPath
                : baseUrl + ensureLeadingSlash(absoluteOrRootedPath);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("X-Auth-Token", token)
                .GET()
                .build();

        HttpResponse<String> res = send(req);
        if (res.statusCode() / 100 != 2) {
            throw new Exception("GET failed: HTTP " + res.statusCode() + " - " + res.body());
        }
        return res.body();
    }

    /** Add device (requires appropriate role; will fail on read-only sandbox). */
    public static String addDeviceRaw(AddDeviceRequest body) throws Exception {
        String token = getToken();
        String url = baseUrl + "/dna/intent/api/v1/network-device";
        String json = mapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Auth-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> res = send(req);
        if (res.statusCode() / 100 != 2) {
            throw new Exception("Add device failed: HTTP " + res.statusCode() + " - " + res.body());
        }
        return res.body();
    }

    private static HttpResponse<String> send(HttpRequest req) throws Exception {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new Exception("Request timed out: " + req.uri());
        }
    }

    private static String required(Properties p, String key) throws Exception {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) throw new Exception("Missing property: " + key);
        return v.trim();
    }

    private static String trimTrailingSlash(String s) {
        return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }

    private static String ensureLeadingSlash(String s) {
        return s.startsWith("/") ? s : "/" + s;
    }

    // ====== DTOs ======

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TokenResponse {
        @JsonProperty("Token")
        public String Token;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NetworkDeviceResponse {
        public List<Device> response;
        public String version;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Device {
        public String id;
        public String hostname;
        public String managementIpAddress;
        public String type;
        public String softwareVersion;
    }

    /** Body for POST /dna/intent/api/v1/network-device */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddDeviceRequest {
        public String[] ipAddress;
        public String snmpVersion;       // "v2" or "v3"
        public String snmpROCommunity;
        public String snmpRWCommunity;
        public String cliTransport;      // "ssh"
        public String userName;
        public String password;
        public String enablePassword;
    }
}