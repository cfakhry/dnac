package com.dnac;

import com.dnac.sdk.DnacClient;
import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.http.HttpExecutor;
import com.dnac.sdk.http.JsonSupport;
import com.dnac.sdk.model.common.CountResponse;

import java.lang.reflect.Field;
import java.util.Map;

class MiscApi {
    private final DnacConfig cfg;
    private final HttpExecutor http;
    private final JsonSupport json = new JsonSupport();

    MiscApi(DnacConfig cfg, DnacClient client) throws Exception {
        this.cfg = cfg;
        // Access HttpExecutor via reflection, or add a proper MiscApi in your SDK.
        Field httpExecField = Class.forName("com.dnac.sdk.DnacClientImpl")
                .getDeclaredField("httpExec");
        httpExecField.setAccessible(true);
        this.http = (HttpExecutor) httpExecField.get(client);
    }

    String getRaw(String rootedPath) throws Exception {
        return http.get(rootedPath, null);
    }

    CountResponse getCount(String rootedPath) throws Exception {
        String body = http.get(rootedPath, Map.of());
        return json.read(body, CountResponse.class);
    }
}