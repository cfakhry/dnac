// com/dnac/sdk/exceptions/DnacTransportException.java
package com.dnac.sdk.exceptions;

import java.net.URI;

public class DnacTransportException extends DnacException {
    public DnacTransportException(URI uri, String message, Throwable cause) {
        super(uri, message, null, cause);
    }
}
