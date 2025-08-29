package com.dnac.sdk.model.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandRunnerSubmitResponse {
    public SubmitResponse response;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmitResponse {
        public String taskId;
        public String url;
    }
}