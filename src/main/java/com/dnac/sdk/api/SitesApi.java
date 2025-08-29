// com/dnac/sdk/api/SitesApi.java
package com.dnac.sdk.api;

import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.http.HttpExecutor;
import com.dnac.sdk.http.JsonSupport;
import com.dnac.sdk.model.site.SiteListResponse;

public final class SitesApi {
    private final HttpExecutor http;

    public SitesApi(DnacConfig cfg, HttpExecutor http) { this.http = http; }

    public SiteListResponse list() throws Exception {
        String json = http.get("/dna/intent/api/v1/site", null);
        return new JsonSupport().read(json, SiteListResponse.class);
    }
}