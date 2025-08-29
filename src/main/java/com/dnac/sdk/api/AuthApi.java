// com/dnac/sdk/api/AuthApi.java
package com.dnac.sdk.api;

import com.dnac.sdk.auth.TokenProvider;

public final class AuthApi {
    private final TokenProvider tokens;

    public AuthApi(TokenProvider tokens) { this.tokens = tokens; }

    /** Returns a current valid token (refreshes if expired). */
    public String getToken() throws Exception {
        return tokens.getToken();
    }
}
