// com/dnac/sdk/DnacClient.java
package com.dnac.sdk;

import com.dnac.sdk.api.CommandRunnerApi;
import com.dnac.sdk.api.DevicesApi;
import com.dnac.sdk.api.SitesApi;
import com.dnac.sdk.api.TemplatesApi;

public interface DnacClient extends AutoCloseable {
    DevicesApi devices();
    SitesApi sites();
    TemplatesApi templates();
    CommandRunnerApi commandRunner();

    @Override default void close() {}
}