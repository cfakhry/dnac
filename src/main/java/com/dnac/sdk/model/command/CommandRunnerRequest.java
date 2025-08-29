package com.dnac.sdk.model.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandRunnerRequest {
    public List<String> commands;
    public List<String> deviceUuids;
    public int timeout;
}