// com/dnac/sdk/DnacClientImpl.java
package com.dnac.sdk;

import com.dnac.sdk.api.*;
import com.dnac.sdk.auth.*;
import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.config.HttpClientFactory;
import com.dnac.sdk.http.HttpExecutor;
import com.dnac.sdk.http.JsonSupport;

import java.net.http.HttpClient;
import java.time.Duration;

public final class DnacClientImpl implements DnacClient {
    private final HttpClient http;
    private final TokenProvider tokenProvider;
    private final HttpExecutor httpExec;

    private final DevicesApi devices;
    private final SitesApi sites;
    private final TemplatesApi templates;
    private final CommandRunnerApi commandRunner;

    public DnacClientImpl(DnacConfig cfg) throws Exception {
        this.http = HttpClientFactory.create(cfg);
        JsonSupport json = new JsonSupport(); // central ObjectMapper

        TokenProvider base = new BasicAuthTokenProvider(cfg, http, json);
        this.tokenProvider = new CachingTokenProvider(base, Duration.ofMinutes(55));

        this.httpExec = new HttpExecutor(cfg, http, tokenProvider, json);

        this.devices       = new DevicesApi(cfg, httpExec);
        this.sites         = new SitesApi(cfg, httpExec);
        this.templates     = new TemplatesApi(cfg, httpExec);
        this.commandRunner = new CommandRunnerApi(cfg, httpExec);
    }

    public DevicesApi devices()       { return devices; }
    public SitesApi sites()           { return sites; }
    public TemplatesApi templates()   { return templates; }
    public CommandRunnerApi commandRunner() { return commandRunner; }
}