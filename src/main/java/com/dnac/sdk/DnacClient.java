// com/dnac/sdk/DnacClient.java
package com.dnac.sdk;

import com.dnac.sdk.api.*;

public interface DnacClient extends AutoCloseable {
    DevicesApi devices();
    SitesApi sites();
    TemplatesApi templates();
    CommandRunnerApi commandRunner();

    // NEW: official, no reflection
    AuthApi auth();
    MiscApi misc();

    @Override default void close() {}
}
