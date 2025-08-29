// com/dnac/sdk/api/CommandRunnerApi.java
package com.dnac.sdk.api;

import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.http.HttpExecutor;
import com.dnac.sdk.http.JsonSupport;
import com.dnac.sdk.model.command.*;
import com.dnac.sdk.model.common.TaskEnvelope;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class CommandRunnerApi {
    private final HttpExecutor http;
    private final JsonSupport json = new JsonSupport();

    public CommandRunnerApi(DnacConfig cfg, HttpExecutor http) { this.http = http; }

    public String submit(List<String> deviceUuids, List<String> commands, int timeoutSec) throws Exception {
        CommandRunnerRequest req = new CommandRunnerRequest();
        req.deviceUuids = deviceUuids;
        req.commands = commands;
        req.timeout = timeoutSec;

        String body = http.postJson("/dna/intent/api/v1/network-device-poller/cli/read-request", req);
        CommandRunnerSubmitResponse cr = json.read(body, CommandRunnerSubmitResponse.class);
        if (cr.response == null || cr.response.taskId == null || cr.response.taskId.isBlank())
            throw new IllegalStateException("Missing taskId");
        return cr.response.taskId;
    }

    public String getTask(String taskId) throws Exception {
        return http.get("/dna/intent/api/v1/task/" + urlEnc(taskId), null);
    }

    public String waitForFileId(String taskId, Duration maxWait, Duration pollInterval) throws Exception {
        Instant deadline = Instant.now().plus(maxWait);
        while (Instant.now().isBefore(deadline)) {
            TaskEnvelope env = json.read(getTask(taskId), TaskEnvelope.class);
            if (env.response != null && env.response.progress != null) {
                try {
                    JsonNode p = json.mapper().readTree(env.response.progress);
                    JsonNode file = p.get("fileId");
                    if (file != null && !file.asText().isBlank()) return file.asText();
                } catch (Exception ignore) {}
            }
            Thread.sleep(pollInterval.toMillis());
        }
        throw new RuntimeException("Timed out waiting for fileId in task " + taskId);
    }

    public String getFile(String fileId) throws Exception {
        return http.get("/dna/intent/api/v1/file/" + urlEnc(fileId), null);
    }

    /** Convenience matching your original helper */
    public Optional<String> runShowIpIntBriefOnPlatform(String platformId,
                                                        DevicesApi devicesApi) throws Exception {
        var ids = devicesApi.idsByPlatformId(platformId);
        if (ids.isEmpty()) return Optional.empty();

        String taskId = submit(ids, List.of("show version", "show ip int brief"), 0);
        String fileId = waitForFileId(taskId, Duration.ofSeconds(60), Duration.ofSeconds(2));
        String raw = getFile(fileId);

        JsonNode arr = json.mapper().readTree(raw);
        if (!arr.isArray() || arr.size() == 0) return Optional.empty();
        JsonNode success = arr.get(0).path("commandResponses").path("SUCCESS");
        JsonNode showIp = success.get("show ip int brief");
        if (showIp == null || showIp.isNull()) return Optional.empty();
        if (showIp.isTextual()) return Optional.of(showIp.asText());
        if (showIp.isArray()) {
            String joined = new StringBuilder()
                    .append(String.join("\n\n",
                            java.util.stream.StreamSupport.stream(showIp.spliterator(), false)
                                    .map(JsonNode::asText).toList()))
                    .toString();
            return Optional.of(joined);
        }
        return Optional.of(showIp.toString());
    }

    private static String urlEnc(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}