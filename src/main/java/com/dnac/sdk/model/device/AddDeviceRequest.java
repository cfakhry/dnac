package com.dnac.sdk.model.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddDeviceRequest {
    public String[] ipAddress;
    public String snmpVersion;       // "v2" or "v3"
    public String snmpROCommunity;
    public String snmpRWCommunity;
    public String cliTransport;      // "ssh"
    public String userName;
    public String password;
    public String enablePassword;
}