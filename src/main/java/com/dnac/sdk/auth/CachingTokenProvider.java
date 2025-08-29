// com/dnac/sdk/auth/CachingTokenProvider.java
package com.dnac.sdk.auth;

import java.time.*;

public final class CachingTokenProvider implements TokenProvider {
    private final TokenProvider delegate;
    private final Duration ttl;
    private volatile String cached;
    private volatile Instant exp = Instant.EPOCH;

    public CachingTokenProvider(TokenProvider delegate, Duration ttl) {
        this.delegate = delegate; this.ttl = ttl;
    }

    @Override public synchronized String getToken() throws Exception {
        if (cached != null && Instant.now().isBefore(exp)) return cached;
        cached = delegate.getToken();
        exp = Instant.now().plus(ttl);
        return cached;
    }
}