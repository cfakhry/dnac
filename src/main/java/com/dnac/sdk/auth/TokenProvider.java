// com/dnac/sdk/auth/TokenProvider.java
package com.dnac.sdk.auth;

public interface TokenProvider {
    String getToken() throws Exception;
}