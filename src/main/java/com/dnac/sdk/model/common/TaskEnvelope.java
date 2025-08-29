package com.dnac.sdk.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskEnvelope {
    public TaskInner response;
    public String version;
}