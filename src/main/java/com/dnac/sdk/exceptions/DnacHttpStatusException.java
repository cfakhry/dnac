// com/dnac/sdk/exceptions/DnacHttpStatusException.java
package com.dnac.sdk.exceptions;

import java.net.URI;

public class DnacHttpStatusException extends DnacException {
    private final int statusCode;
    private final ApiErrorPayload payload;

    public DnacHttpStatusException(URI uri, int statusCode, ApiErrorPayload payload, String requestId) {
        super(uri, buildMessage(statusCode, payload), requestId);
        this.statusCode = statusCode;
        this.payload = payload;
    }

    private static String buildMessage(int status, ApiErrorPayload p) {
        String base = "HTTP " + status;
        if (p != null) {
            String msg = (p.message != null ? p.message : p.detail);
            if (msg != null && !msg.isBlank()) return base + " - " + msg;
        }
        return base;
    }

    public int statusCode() { return statusCode; }
    public ApiErrorPayload payload() { return payload; }
}
