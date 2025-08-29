// com/dnac/sdk/api/DevicesApi.java
package com.dnac.sdk.api;

import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.http.HttpExecutor;
import com.dnac.sdk.http.JsonSupport;
import com.dnac.sdk.model.device.*;

import java.util.*;
import java.util.stream.Collectors;

public final class DevicesApi {
    private final DnacConfig cfg;
    private final HttpExecutor http;

    public DevicesApi(DnacConfig cfg, HttpExecutor http) {
        this.cfg = cfg; this.http = http;
    }

    public List<Device> listAll() throws Exception {
        String json = http.get("/dna/intent/api/v1/network-device", null);
        NetworkDeviceResponse resp = new JsonSupport().read(json, NetworkDeviceResponse.class);
        return resp.response == null ? List.of() : resp.response;
    }

    public Device getById(String id) throws Exception {
        String json = http.get("/dna/intent/api/v1/network-device/" + id, null);
        return new JsonSupport().read(json, DeviceResponse.class).response;
    }

    public Device getBySerial(String serial) throws Exception {
        String json = http.get("/dna/intent/api/v1/network-device/serial-number/" + serial, null);
        return new JsonSupport().read(json, DeviceResponse.class).response;
    }

    public int count() throws Exception {
        String json = http.get("/dna/intent/api/v1/network-device/count", null);
        // simple, not worth a DTO:
        com.fasterxml.jackson.databind.JsonNode root = new JsonSupport().mapper().readTree(json);
        return root.path("response").asInt();
    }

    public List<String> idsByPlatformId(String platformId) throws Exception {
        if (platformId == null || platformId.isBlank()) return List.of();
        String json = http.get("/dna/intent/api/v1/network-device", Map.of("platformId", platformId));
        NetworkDeviceResponse resp = new JsonSupport().read(json, NetworkDeviceResponse.class);
        if (resp.response == null) return List.of();
        return resp.response.stream().map(d -> d.id).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public String addDeviceRaw(AddDeviceRequest body) throws Exception {
        // endpoint: POST /dna/intent/api/v1/network-device
        // Returns raw JSON string from DNAC so callers can see the task response
        return http.postJson("/dna/intent/api/v1/network-device", body);
    }
}