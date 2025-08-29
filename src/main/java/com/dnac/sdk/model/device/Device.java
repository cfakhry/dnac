package com.dnac.sdk.model.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {
    public String id;
    public String hostname;
    public String managementIpAddress;
    public String type;
    public String softwareVersion;
    public String serialNumber;
    public String platformId;
}