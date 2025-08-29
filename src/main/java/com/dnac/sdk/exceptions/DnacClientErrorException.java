// com/dnac/sdk/exceptions/DnacClientErrorException.java
package com.dnac.sdk.exceptions;

import java.net.URI;

public class DnacClientErrorException extends DnacHttpStatusException {
    public DnacClientErrorException(URI uri, int status, ApiErrorPayload payload, String requestId) {
        super(uri, status, payload, requestId);
    }
}
