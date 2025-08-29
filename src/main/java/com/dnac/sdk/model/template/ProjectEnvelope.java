package com.dnac.sdk.model.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectEnvelope {
    public Project response;   // commonly present
    public String  version;
}