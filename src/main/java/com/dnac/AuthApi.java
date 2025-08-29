package com.dnac;

import com.dnac.sdk.DnacClient;
import com.dnac.sdk.auth.TokenProvider;
import com.dnac.sdk.config.DnacConfig;

import java.lang.reflect.Field;

class AuthApi {
    private final DnacConfig cfg;
    private final DnacClient client;

    AuthApi(DnacConfig cfg, DnacClient client) {
        this.cfg = cfg; this.client = client;
    }

    String currentToken() throws Exception {
        // Access the TokenProvider via reflection or expose it in your SDK if you prefer.
        Field httpExecField = Class.forName("com.dnac.sdk.DnacClientImpl")
                .getDeclaredField("tokenProvider");
        httpExecField.setAccessible(true);
        TokenProvider provider = (TokenProvider) httpExecField.get(client);
        return provider.getToken();
    }
}