// com/dnac/sdk/api/MiscApi.java
package com.dnac.sdk.api;

import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.http.HttpExecutor;
import com.dnac.sdk.http.JsonSupport;
import com.dnac.sdk.model.common.CountResponse;

import java.util.Map;

public final class MiscApi {
    private final DnacConfig cfg;
    private final HttpExecutor http;
    private final JsonSupport json;

    public MiscApi(DnacConfig cfg, HttpExecutor http, JsonSupport json) {
        this.cfg = cfg; this.http = http; this.json = json;
    }

    public String getRaw(String rootedPath) throws Exception {
        return http.get(rootedPath, null);
    }

    public CountResponse getCount(String rootedPath) throws Exception {
        String body = http.get(rootedPath, Map.of());
        return json.read(body, CountResponse.class);
    }
}
