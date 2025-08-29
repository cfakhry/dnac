package com.dnac.sdk.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal placeholder if you decide to parse DNAC error envelopes. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiError {
    public String errorCode;
    public String message;
    public String detail;
}