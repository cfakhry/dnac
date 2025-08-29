package com.dnac;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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

    /** Get all devices (no filters). */
    public static List<Device> getDevices() throws Exception {
        String json = getRaw("/dna/intent/api/v1/network-device");
        NetworkDeviceResponse resp = mapper.readValue(json, NetworkDeviceResponse.class);
        return resp.response == null ? List.of() : resp.response;
    }

    /** Get Sites */
    public static List<Site> getSites() throws Exception{
        String json = getRaw("/dna/intent/api/v1/site");
        SiteListResponse resp = mapper.readValue(json, SiteListResponse.class);
        return resp.response == null ? List.of() : resp.response;
    }

    /** Get device IDs filtered by platformId (e.g., "C9500-40X"), mirroring the Python query. */
    public static List<String> getDeviceIdsByPlatformId(String platformId) throws Exception {
        if (platformId == null || platformId.isBlank()) return List.of();
        String path = "/dna/intent/api/v1/network-device";
        Map<String, String> qs = Map.of("platformId", platformId);
        String json = getRawWithQuery(path, qs);

        NetworkDeviceResponse resp = mapper.readValue(json, NetworkDeviceResponse.class);
        if (resp.response == null) return List.of();
        return resp.response.stream()
                .map(d -> d.id)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** GET raw JSON with auth, path can be absolute or rooted. */
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

    /** GET raw JSON with query params. */
    public static String getRawWithQuery(String rootedPath, Map<String, String> query) throws Exception {
        String token = getToken();
        String base = baseUrl + ensureLeadingSlash(rootedPath);
        String q = "";
        if (query != null && !query.isEmpty()) {
            q = "?" + query.entrySet().stream()
                    .map(e -> urlEnc(e.getKey()) + "=" + urlEnc(e.getValue()))
                    .collect(Collectors.joining("&"));
        }
        String url = base + q;

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

    /** Body for POST /dna/intent/api/v1/network-device */
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

    // ======== NEW: Command Runner helpers (Python parity) ========

    /** Submit CLI commands to device UUIDs. Returns the created taskId. */
    public static String submitCommands(List<String> deviceUuids, List<String> commands, int timeoutSeconds) throws Exception {
        if (deviceUuids == null || deviceUuids.isEmpty()) throw new Exception("deviceUuids required");
        if (commands == null || commands.isEmpty()) throw new Exception("commands required");

        String token = getToken();
        String url = baseUrl + "/dna/intent/api/v1/network-device-poller/cli/read-request";

        CommandRunnerRequest body = new CommandRunnerRequest();
        body.commands = commands;
        body.deviceUuids = deviceUuids;
        body.timeout = timeoutSeconds;

        String jsonBody = mapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Auth-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> res = send(req);
        if (res.statusCode() / 100 != 2) {
            throw new Exception("Command submit failed: HTTP " + res.statusCode() + " - " + res.body());
        }

        CommandRunnerSubmitResponse cr = mapper.readValue(res.body(), CommandRunnerSubmitResponse.class);
        if (cr.response == null || cr.response.taskId == null || cr.response.taskId.isBlank()) {
            throw new Exception("Command submit response missing taskId");
        }
        return cr.response.taskId;
    }

    /** GET task by ID. Raw JSON. */
    public static String getTaskRaw(String taskId) throws Exception {
        return getRaw("/dna/intent/api/v1/task/" + urlEnc(taskId));
    }

    /** Poll task until a fileId appears in response.progress JSON, or timeout is reached. */
    public static String waitForFileIdFromTask(String taskId, Duration maxWait, Duration pollInterval) throws Exception {
        Instant deadline = Instant.now().plus(maxWait);
        while (Instant.now().isBefore(deadline)) {
            String json = getTaskRaw(taskId);
            TaskEnvelope env = mapper.readValue(json, TaskEnvelope.class);
            if (env.response != null && env.response.progress != null) {
                try {
                    JsonNode progress = mapper.readTree(env.response.progress);
                    JsonNode fileIdNode = progress.get("fileId");
                    if (fileIdNode != null && !fileIdNode.asText().isBlank()) {
                        return fileIdNode.asText();
                    }
                } catch (Exception ignore) {
                    // progress may not yet be JSON with fileId
                }
            }
            Thread.sleep(pollInterval.toMillis());
        }
        throw new Exception("Timed out waiting for fileId in task " + taskId);
    }

    /** GET command results by fileId. Raw JSON (DNAC returns an array). */
    public static String getFileRaw(String fileId) throws Exception {
        return getRaw("/dna/intent/api/v1/file/" + urlEnc(fileId));
    }

    /**
     * Convenience: run commands on platformId-filtered devices and return
     * the "show ip int brief" output (first SUCCESS block), matching the Python example.
     */
    public static Optional<String> runShowIpIntBriefOnPlatform(String platformId) throws Exception {
        List<String> deviceIds = getDeviceIdsByPlatformId(platformId);
        if (deviceIds.isEmpty()) return Optional.empty();

        String taskId = submitCommands(deviceIds, List.of("show version", "show ip int brief"), 0);
        // Python sleeps ~10s; here we poll up to 60s, every 2s
        String fileId = waitForFileIdFromTask(taskId, Duration.ofSeconds(60), Duration.ofSeconds(2));
        String raw = getFileRaw(fileId);

        // DNAC returns an array; find response[0].commandResponses.SUCCESS["show ip int brief"]
        JsonNode arr = mapper.readTree(raw);
        if (!arr.isArray() || arr.size() == 0) return Optional.empty();
        JsonNode first = arr.get(0);
        JsonNode success = first.path("commandResponses").path("SUCCESS");
        if (success.isMissingNode()) return Optional.empty();

        JsonNode showIp = success.get("show ip int brief");
        if (showIp == null || showIp.isNull()) return Optional.empty();

        if (showIp.isTextual()) return Optional.of(showIp.asText());
        // sometimes DNAC returns arrays per device; join if so
        if (showIp.isArray()) {
            String joined = new StringBuilder()
                    .append(String.join("\n\n",
                            iterable(showIp).stream()
                                    .map(JsonNode::asText)
                                    .collect(Collectors.toList())))
                    .toString();
            return Optional.of(joined);
        }
        return Optional.of(showIp.toString());
    }

    private static List<JsonNode> iterable(JsonNode arrayNode) {
        List<JsonNode> out = new ArrayList<>();
        arrayNode.forEach(out::add);
        return out;
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

    private static String urlEnc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
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
    static class SiteListResponse {
        public List<Site> response;
        public String version;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Site {
        public String id;
        public String name;
        public String instanceTenantId;
        public String siteHierarchy;
        public String siteNameHierarchy;

        // additionalInfo may contain arbitrary objects, so keep it generic
        public List<Object> additionalInfo;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Device {
        public String id;
        public String hostname;
        public String managementIpAddress;
        public String type;
        public String softwareVersion;
        public String platformId; // useful when filtering
    }

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdditionalInfo {
        public String name;
        public String value;
    }

    // ----- Command Runner DTOs -----

    /** POST body for CLI read-request. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommandRunnerRequest {
        public List<String> commands;
        public List<String> deviceUuids;
        public int timeout;
    }

    /** Response from CLI read-request submit. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommandRunnerSubmitResponse {
        public SubmitResponse response;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SubmitResponse {
            public String taskId;
            public String url;
        }
    }

    /** Envelope for /task/{taskId} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskEnvelope {
        public TaskInner response;
        public String version;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskInner {
        public String progress;     // JSON string (later contains {"fileId": "...", ...})
        public Boolean isError;
        public String startTime;
        public String endTime;
        public String lastUpdate;
        public String version;
        public String failureReason;
    }
}