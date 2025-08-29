// com/dnac/sdk/api/DevicesApi.java
package com.dnac.sdk.api;

import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.http.HttpExecutor;
import com.dnac.sdk.http.JsonSupport;
import com.dnac.sdk.model.common.CountResponse;
import com.dnac.sdk.model.device.*;

import java.util.*;
import java.util.stream.Collectors;

public final class DevicesApi {
    private final DnacConfig cfg;
    private final HttpExecutor http;
    private final JsonSupport json = new JsonSupport();

    public DevicesApi(DnacConfig cfg, HttpExecutor http) {
        this.cfg = cfg; this.http = http;
    }

    public List<Device> listAll() throws Exception {
        String body = http.get("/dna/intent/api/v1/network-device", null);
        NetworkDeviceResponse resp = json.read(body, NetworkDeviceResponse.class);
        return resp.response == null ? List.of() : resp.response;
    }

    public Device getById(String id) throws Exception {
        String body = http.get("/dna/intent/api/v1/network-device/" + id, null);
        return json.read(body, DeviceResponse.class).response;
    }

    public Device getBySerial(String serial) throws Exception {
        String body = http.get("/dna/intent/api/v1/network-device/serial-number/" + serial, null);
        return json.read(body, DeviceResponse.class).response;
    }

    public int count() throws Exception {
        String body = http.get("/dna/intent/api/v1/network-device/count", null);
        return json.read(body, CountResponse.class).response;
    }

    public List<String> idsByPlatformId(String platformId) throws Exception {
        if (platformId == null || platformId.isBlank()) return List.of();
        String body = http.get("/dna/intent/api/v1/network-device", Map.of("platformId", platformId));
        NetworkDeviceResponse resp = json.read(body, NetworkDeviceResponse.class);
        if (resp.response == null) return List.of();
        return resp.response.stream().map(d -> d.id).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** Returns raw JSON from DNAC (task submission result). Consider modeling if you need fields. */
    public String addDeviceRaw(AddDeviceRequest req) throws Exception {
        return http.postJson("/dna/intent/api/v1/network-device", req);
    }
}
