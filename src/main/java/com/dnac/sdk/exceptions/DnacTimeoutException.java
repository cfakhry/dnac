// com/dnac/sdk/exceptions/DnacTimeoutException.java
package com.dnac.sdk.exceptions;

import java.net.URI;

public class DnacTimeoutException extends DnacException {
    public DnacTimeoutException(URI uri, String message, Throwable cause) {
        super(uri, message, null, cause);
    }
}
