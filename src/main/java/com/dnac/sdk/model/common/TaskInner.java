package com.dnac.sdk.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskInner {
    public String progress;     // often JSON string containing {"fileId": "..."}
    public Boolean isError;
    public String startTime;
    public String endTime;
    public String lastUpdate;
    public String version;
    public String failureReason;
}