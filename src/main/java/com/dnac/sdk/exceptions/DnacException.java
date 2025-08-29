// com/dnac/sdk/exceptions/DnacException.java
package com.dnac.sdk.exceptions;

import java.net.URI;

public class DnacException extends RuntimeException {
    private final URI uri;
    private final String requestId;

    public DnacException(URI uri, String message, String requestId, Throwable cause) {
        super(message, cause);
        this.uri = uri;
        this.requestId = requestId;
    }
    public DnacException(URI uri, String message, String requestId) {
        this(uri, message, requestId, null);
    }
    public URI uri() { return uri; }
    public String requestId() { return requestId; }
}
