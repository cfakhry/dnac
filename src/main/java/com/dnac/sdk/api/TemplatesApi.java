// com/dnac/sdk/api/TemplatesApi.java
package com.dnac.sdk.api;

import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.http.HttpExecutor;
import com.dnac.sdk.http.JsonSupport;
import com.dnac.sdk.model.template.*;

import java.util.List;

public final class TemplatesApi {
    private final HttpExecutor http;
    private final JsonSupport json = new JsonSupport();

    public TemplatesApi(DnacConfig cfg, HttpExecutor http) { this.http = http; }

    public List<Project> listProjects() throws Exception {
        String body = http.get("/dna/intent/api/v1/template-programmer/project", null);
        var type = json.mapper().getTypeFactory().constructCollectionType(List.class, Project.class);
        return json.mapper().readValue(body, type);
    }

    public Project createProject(String name, String description, List<CreateProjectRequest.Tag> tags) throws Exception {
        CreateProjectRequest req = new CreateProjectRequest();
        req.name = name;
        req.description = description == null ? "" : description;
        req.tags = tags == null ? List.of() : tags;

        String body = http.postJson("/dna/intent/api/v1/template-programmer/project", req);

        // some deployments wrap in envelope
        try {
            ProjectEnvelope env = json.read(body, ProjectEnvelope.class);
            if (env.response != null) return env.response;
        } catch (Exception ignore) {}
        return json.read(body, Project.class);
    }
}