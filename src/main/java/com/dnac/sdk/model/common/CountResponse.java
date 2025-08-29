package com.dnac.sdk.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Standard DNAC count envelope: { "response": <int>, "version": "..." } */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CountResponse {
    public int response;
    public String version;
}