// com/dnac/sdk/exceptions/ApiErrorPayload.java
package com.dnac.sdk.exceptions;

import com.dnac.sdk.http.JsonSupport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal DNAC error envelope catcher; extend with fields you observe. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiErrorPayload {
    public String errorCode;
    public String message;
    public String detail;

    public static ApiErrorPayload tryParse(String body, JsonSupport json) {
        if (body == null || body.isBlank()) return null;
        try {
            return json.read(body, ApiErrorPayload.class);
        } catch (Exception ignore) {
            return null;
        }
    }
}
