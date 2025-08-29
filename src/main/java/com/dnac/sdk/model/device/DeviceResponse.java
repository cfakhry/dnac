package com.dnac.sdk.model.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceResponse {
    public Device response;
}