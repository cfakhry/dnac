// com/dnac/sdk/exceptions/DnacServerErrorException.java
package com.dnac.sdk.exceptions;

import java.net.URI;

public class DnacServerErrorException extends DnacHttpStatusException {
    public DnacServerErrorException(URI uri, int status, ApiErrorPayload payload, String requestId) {
        super(uri, status, payload, requestId);
    }
}
