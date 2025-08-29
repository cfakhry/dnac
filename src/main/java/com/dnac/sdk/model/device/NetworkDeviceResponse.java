package com.dnac.sdk.model.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkDeviceResponse {
    public List<Device> response;
    public String version;
}